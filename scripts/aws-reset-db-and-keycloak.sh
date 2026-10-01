#!/bin/bash
# ==============================================================================
# CGV CINEMA PLATFORM - AWS RDS & KEYCLOAK AUTOMATED RESET & SEED SCRIPT
# ==============================================================================
# Mục đích:
# 1. Reset sạch toàn bộ database nghiệp vụ (schema public) trên AWS RDS PostgreSQL.
# 2. Khởi tạo lại toàn bộ 25 bảng chuẩn xác qua Hibernate DDL.
# 3. Nạp Master Seed Data (Phim, Rạp, Ghế, Suất chiếu, Khuyến mãi...).
# 4. Reset schema keycloak và tự động Import lại toàn bộ Realm & Tài khoản mẫu.
# ==============================================================================

set -e

NAMESPACE="cgv-prod"
RDS_HOST="cgv-db-enterprise.c1g082ggo02h.ap-southeast-1.rds.amazonaws.com"
DB_NAME="cgv"
DB_USER="postgres"
DB_PASS="CGVPassword2026"
CLIENT_POD="pgclient-reset-job"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
SQL_SEED_FILE="${SCRIPT_DIR}/seed-data.sql"
REALM_PROD_FILE="${ROOT_DIR}/keycloak/cgv-realm-export.prod.json"

echo "======================================================================"
echo "🎬 CGV ENTERPRISE - BẮT ĐẦU RESET DB VÀ KEYCLOAK TRÊN AWS EKS"
echo "======================================================================"
echo "📍 Cluster Namespace: ${NAMESPACE}"
echo "📍 RDS Endpoint     : ${RDS_HOST}"
echo "📍 Database Name    : ${DB_NAME}"
echo "======================================================================"

# ------------------------------------------------------------------------------
# BƯỚC 1: KHỞI TẠO POD POSTGRESQL CLIENT TẠM THỜI TRONG CỤM K8S
# ------------------------------------------------------------------------------
echo ""
echo "🚀 [1/6] Đang kiểm tra và khởi tạo pod client tạm thời '${CLIENT_POD}'..."
if kubectl get pod "${CLIENT_POD}" -n "${NAMESPACE}" > /dev/null 2>&1; then
    echo "⚠️  Pod '${CLIENT_POD}' đã tồn tại, tiến hành xóa để tạo mới..."
    kubectl delete pod "${CLIENT_POD}" -n "${NAMESPACE}" --grace-period=0 --force > /dev/null 2>&1
fi

kubectl run "${CLIENT_POD}" \
    --image=postgres:16-alpine \
    --restart=Never \
    -n "${NAMESPACE}" \
    -- sleep 3600

echo "⏳ Đang chờ pod '${CLIENT_POD}' sẵn sàng..."
kubectl wait --for=condition=Ready pod/"${CLIENT_POD}" -n "${NAMESPACE}" --timeout=60s

# ------------------------------------------------------------------------------
# BƯỚC 2: RESET SCHEMA PUBLIC TRÊN AWS RDS
# ------------------------------------------------------------------------------
echo ""
echo "🧹 [2/6] Đang xóa và tạo mới schema 'public' trên RDS PostgreSQL..."
kubectl exec -i "${CLIENT_POD}" -n "${NAMESPACE}" -- sh -c \
    "PGPASSWORD='${DB_PASS}' psql -h '${RDS_HOST}' -U '${DB_USER}' -d '${DB_NAME}' -c '
        DROP SCHEMA IF EXISTS public CASCADE;
        CREATE SCHEMA public;
        GRANT ALL ON SCHEMA public TO postgres;
        GRANT ALL ON SCHEMA public TO public;
    '"
echo "✅ Đã reset sạch schema 'public' thành công!"

# ------------------------------------------------------------------------------
# BƯỚC 3: RESTART MICROSERVICES ĐỂ HIBERNATE TỰ ĐỘNG TẠO LẠI CÁC BẢNG (DDL AUTO)
# ------------------------------------------------------------------------------
echo ""
echo "🔄 [3/6] Đang khởi động lại các Microservices để Hibernate tự động sinh cấu trúc bảng..."
kubectl rollout restart deployment \
    identity-service \
    catalog-service \
    booking-service \
    payment-service \
    marketing-service \
    -n "${NAMESPACE}"

echo "⏳ Đang chờ các dịch vụ cốt lõi sẵn sàng tạo bảng..."
kubectl rollout status deployment/catalog-service -n "${NAMESPACE}" --timeout=120s
kubectl rollout status deployment/identity-service -n "${NAMESPACE}" --timeout=120s

# Chờ 5 giây để Hibernate hoàn tất việc commit cấu trúc bảng
sleep 5

# ------------------------------------------------------------------------------
# BƯỚC 4: NẠP MASTER SEED DATA VÀO DATABASE
# ------------------------------------------------------------------------------
echo ""
echo "📦 [4/6] Đang nạp Master Seed Data (Rạp, Phòng, Ghế, Phim, Suất chiếu, Khuyến mãi)..."
kubectl cp "${SQL_SEED_FILE}" "${NAMESPACE}/${CLIENT_POD}:/tmp/seed-data.sql"

kubectl exec -i "${CLIENT_POD}" -n "${NAMESPACE}" -- sh -c \
    "PGPASSWORD='${DB_PASS}' psql -h '${RDS_HOST}' -U '${DB_USER}' -d '${DB_NAME}' -f /tmp/seed-data.sql"

echo "✅ Đã nạp thành công toàn bộ Master Seed Data vào RDS!"

# ------------------------------------------------------------------------------
# BƯỚC 5: RESET VÀ IMPORT LẠI KEYCLOAK REALM & TÀI KHOẢN MẪU
# ------------------------------------------------------------------------------
echo ""
echo "🔐 [5/6] Đang tiến hành reset Keycloak IAM & Nạp lại Realm mẫu..."

# 5.1 Cập nhật ConfigMap keycloak-realm-config với file export mới nhất
if [ -f "${REALM_PROD_FILE}" ]; then
    echo "📄 Cập nhật ConfigMap 'keycloak-realm-config' từ '${REALM_PROD_FILE}'..."
    kubectl create configmap keycloak-realm-config \
        --from-file=cgv-realm-export.json="${REALM_PROD_FILE}" \
        -n "${NAMESPACE}" \
        --dry-run=client -o yaml | kubectl apply -f -
fi

# 5.2 Xóa schema keycloak trong RDS
echo "🧹 Xóa schema 'keycloak' cũ trong RDS..."
kubectl exec -i "${CLIENT_POD}" -n "${NAMESPACE}" -- sh -c \
    "PGPASSWORD='${DB_PASS}' psql -h '${RDS_HOST}' -U '${DB_USER}' -d '${DB_NAME}' -c '
        DROP SCHEMA IF EXISTS keycloak CASCADE;
        CREATE SCHEMA keycloak;
    '"

# 5.3 Restart Keycloak Pod để kích hoạt Auto-Import Realm
echo "🔄 Khởi động lại pod Keycloak để auto-import realm..."
kubectl rollout restart deployment/keycloak -n "${NAMESPACE}"
kubectl rollout status deployment/keycloak -n "${NAMESPACE}" --timeout=180s

echo "✅ Keycloak đã hoàn tất việc khởi tạo lại 121 bảng và import cgv-realm!"

# ------------------------------------------------------------------------------
# BƯỚC 6: DỌN DẸP POD CLIENT TẠM THỜI
# ------------------------------------------------------------------------------
echo ""
echo "🧹 [6/6] Đang dọn dẹp pod tạm thời '${CLIENT_POD}'..."
kubectl delete pod "${CLIENT_POD}" -n "${NAMESPACE}" --grace-period=0 --force > /dev/null 2>&1

echo ""
echo "======================================================================"
echo "🎉 HOÀN TẤT RESET DB VÀ KEYCLOAK 100% THÀNH CÔNG!"
echo "======================================================================"
echo "📋 DANH SÁCH TÀI KHOẢN MẪU KEYCLOAK SẴN SÀNG SỬ DỤNG (Mật khẩu: 12345678):"
echo "   1. Super Admin     : superadmin.headquarters.enterprise@cgv.vn"
echo "   2. Cinema Manager  : cinemamanager.vincom.dongkhoi@cgv.vn"
echo "   3. Ticket Staff    : ticketstaff.boxoffice.crescentmall@cgv.vn"
echo "   4. Content Lead    : marketing.contentlead.digital@cgv.vn"
echo "======================================================================"
echo "🌐 FE Admin URL        : https://cgvfeadmin.vercel.app"
echo "🌐 FE User URL         : https://cgvfeuser.vercel.app"
echo "🌐 Gateway Endpoint    : https://d2z63drupmmdh8.cloudfront.net"
echo "======================================================================"
