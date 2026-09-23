#!/bin/bash
# ==============================================================================
# CGV Platform - Seed Data Runner Script
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/seed-data.sql"

echo "=================================================="
echo "🚀 Đang nạp dữ liệu mẫu vào PostgreSQL CGV..."
echo "=================================================="

# Kiểm tra nếu container cgv-postgres đang chạy
if docker ps --format '{{.Names}}' | grep -q "^cgv-postgres$"; then
    echo "📦 Đang thực thi seed-data.sql qua Docker container 'cgv-postgres'..."
    docker exec -i cgv-postgres psql -U postgres -d cgv < "${SQL_FILE}"
    EXIT_CODE=$?
else
    echo "⚠️  Không tìm thấy Docker container 'cgv-postgres' đang chạy."
    echo "🔄 Thử kết nối trực tiếp psql localhost:5432..."
    PGPASSWORD=postgrespassword psql -h localhost -p 5432 -U postgres -d cgv -f "${SQL_FILE}"
    EXIT_CODE=$?
fi

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✅ SEED DỮ LIỆU THÀNH CÔNG 100%!"
    echo "🎉 Bây giờ bạn có thể test đầy đủ các API:"
    echo "   1. Tìm rạp gần tôi: GET /api/v1/cinemas/nearby?lat=10.7769&lon=106.7009"
    echo "   2. Xem suất chiếu Avatar gần tôi: GET /api/v1/showtimes/nearby?movieId=11000000-0000-0000-0000-000000000001&lat=10.7769&lon=106.7009"
    echo "   3. Lịch chiếu tại CGV Đồng Khởi: GET /api/v1/cinemas/c1000000-0000-0000-0000-000000000001/schedule"
    echo "   4. Lấy vé của tôi: GET /api/v1/bookings/my-bookings (với Header X-User-Id: usr-test-001)"
    echo "   5. Khuyến mãi đang hoạt động: GET /api/v1/marketings/promotions/active"
    echo "=================================================="
else
    echo ""
    echo "❌ Có lỗi xảy ra trong quá trình nạp dữ liệu. Vui lòng kiểm tra log ở trên."
    exit $EXIT_CODE
fi
