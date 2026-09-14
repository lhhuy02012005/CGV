/**
 * Main Controller cho CGV Admin Portal
 */
const AdminApp = {
  currentTab: 'dashboard',
  moviesList: [],
  cinemasList: [],
  showtimesList: [],
  bookingsList: [],
  vouchersList: [],

  async init() {
    this.setupNavigation();
    await this.loadAllData();
    this.renderCurrentView();
    this.setupModals();
  },

  async loadAllData() {
    this.moviesList = await AdminApi.getMovies();
    this.cinemasList = await AdminApi.getCinemas();
    this.showtimesList = await AdminApi.getShowtimes();
    this.bookingsList = await AdminApi.getBookings();
    this.vouchersList = await AdminApi.getVouchers();
  },

  setupNavigation() {
    const menuItems = document.querySelectorAll('.sidebar-menu .menu-item');
    menuItems.forEach(item => {
      item.addEventListener('click', (e) => {
        e.preventDefault();
        menuItems.forEach(m => m.classList.remove('active'));
        item.classList.add('active');

        const tab = item.dataset.tab;
        this.currentTab = tab;
        this.renderCurrentView();
      });
    });
  },

  renderCurrentView() {
    const titleEl = document.getElementById('topbar-title');
    const container = document.getElementById('view-container');

    switch (this.currentTab) {
      case 'dashboard':
        titleEl.textContent = "Bảng Điều Khiển Tổng Quan (Dashboard)";
        this.renderDashboard(container);
        break;
      case 'movies':
        titleEl.textContent = "Quản Lý Danh Sách Phim";
        this.renderMoviesView(container);
        break;
      case 'cinemas':
        titleEl.textContent = "Quản Lý Cụm Rạp & Phòng Chiếu";
        this.renderCinemasView(container);
        break;
      case 'showtimes':
        titleEl.textContent = "Lập Lịch & Quản Lý Suất Chiếu";
        this.renderShowtimesView(container);
        break;
      case 'bookings':
        titleEl.textContent = "Quản Lý Đơn Vé & Giao Dịch";
        this.renderBookingsView(container);
        break;
      case 'vouchers':
        titleEl.textContent = "Quản Lý Voucher & Khuyến Mãi";
        this.renderVouchersView(container);
        break;
      default:
        this.renderDashboard(container);
    }
  },

  /* ==========================================================================
     1. DASHBOARD VIEW
     ========================================================================== */
  renderDashboard(container) {
    const stats = AdminData.state.stats;

    container.innerHTML = `
      <!-- KPI Grid -->
      <div class="kpi-grid">
        <div class="kpi-card">
          <div class="kpi-header">
            <span class="kpi-title">DOANH THU HÔM NAY</span>
            <span class="kpi-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect width="20" height="14" x="2" y="5" rx="2"/>
                <line x1="2" x2="22" y1="10" y2="10"/>
              </svg>
            </span>
          </div>
          <div class="kpi-value">${new Intl.NumberFormat('vi-VN').format(stats.todayRevenue)} đ</div>
          <div class="kpi-badge up">↑ +12.4% so với hôm qua</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-header">
            <span class="kpi-title">VÉ ĐÃ BÁN RA</span>
            <span class="kpi-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/>
                <path d="M13 5v2"/><path d="M13 17v2"/><path d="M13 11v2"/>
              </svg>
            </span>
          </div>
          <div class="kpi-value">${stats.todayTickets.toLocaleString()} vé</div>
          <div class="kpi-badge up">↑ +8.2% tuần này</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-header">
            <span class="kpi-title">SUẤT CHIẾU HÔM NAY</span>
            <span class="kpi-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect width="18" height="18" x="3" y="3" rx="2"/><path d="M7 3v18"/><path d="M3 7.5h4"/><path d="M3 12h18"/><path d="M3 16.5h4"/><path d="M17 3v18"/><path d="M17 7.5h4"/><path d="M17 16.5h4"/>
              </svg>
            </span>
          </div>
          <div class="kpi-value">${stats.activeShowtimes} suất</div>
          <div class="kpi-badge info">4 cụm rạp hoạt động</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-header">
            <span class="kpi-title">TỶ LỆ LẤP ĐẦY RẠP</span>
            <span class="kpi-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/>
                <polyline points="16 7 22 7 22 13"/>
              </svg>
            </span>
          </div>
          <div class="kpi-value">${stats.occupancyRate}%</div>
          <div class="kpi-badge up">↑ Cao điểm cuối tuần</div>
        </div>
      </div>

      <!-- Dashboard Row -->
      <div class="dashboard-row">
        <!-- Top Phim Doanh Thu -->
        <div class="dashboard-panel">
          <div class="panel-title">
            <span>TOP PHIM DOANH THU CAO NHẤT</span>
            <button class="btn-tbl-action" onclick="AdminApp.switchTab('movies')">Xem tất cả</button>
          </div>
          <table class="admin-table">
            <thead>
              <tr>
                <th>Phim</th>
                <th>Trạng Thái</th>
                <th>Vé Bán</th>
                <th>Tổng Doanh Thu</th>
              </tr>
            </thead>
            <tbody>
              ${this.moviesList.slice(0, 4).map(m => `
                <tr>
                  <td>
                    <div style="display: flex; align-items: center; gap: 12px;">
                      <img src="${m.posterUrl}" class="table-poster" alt="${m.title}">
                      <div>
                        <strong>${m.title}</strong>
                        <div style="font-size: 11px; color: #64748b;">${m.genre} • ${m.duration}p</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span class="status-pill ${m.status === 'NOW_SHOWING' ? 'now-showing' : 'coming-soon'}">
                      ${m.status === 'NOW_SHOWING' ? 'Đang Chiếu' : 'Sắp Chiếu'}
                    </span>
                  </td>
                  <td>${m.ticketsSold ? m.ticketsSold.toLocaleString() : 0}</td>
                  <td><strong>${new Intl.NumberFormat('vi-VN').format(m.revenue || 0)} đ</strong></td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>

        <!-- Đơn Vé Vừa Đặt -->
        <div class="dashboard-panel">
          <div class="panel-title">
            <span>ĐƠN VÉ MỚI NHẤT</span>
            <button class="btn-tbl-action" onclick="AdminApp.switchTab('bookings')">Xem tất cả</button>
          </div>
          <div style="display: flex; flex-direction: column; gap: 12px;">
            ${this.bookingsList.slice(0, 3).map(b => `
              <div style="padding: 12px; background: #f8fafc; border-radius: 6px; border: 1px solid #e2e8f0;">
                <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                  <strong style="color: var(--cgv-red); font-size: 12px;">${b.id}</strong>
                  <span style="font-size: 11px; color: #64748b;">${b.createdAt}</span>
                </div>
                <div style="font-size: 13px; font-weight: bold;">${b.customerName} - ${b.phone}</div>
                <div style="font-size: 12px; color: #475569; margin: 2px 0;">${b.movieTitle} (${b.seats})</div>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 6px;">
                  <span class="status-pill paid">${b.paymentMethod}</span>
                  <strong style="color: #0f172a;">${new Intl.NumberFormat('vi-VN').format(b.amount)} đ</strong>
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `;
  },

  /* ==========================================================================
     2. MOVIES VIEW
     ========================================================================== */
  renderMoviesView(container) {
    container.innerHTML = `
      <div class="table-controls">
        <div class="search-input-wrap">
          <input type="text" id="search-movie-input" placeholder="Tìm kiếm tên phim, thể loại...">
        </div>
        <button class="btn-add-new" id="btn-open-add-movie">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          <span>Thêm Phim Mới</span>
        </button>
      </div>

      <div class="data-table-wrap">
        <table class="admin-table" id="movies-table">
          <thead>
            <tr>
              <th>Poster</th>
              <th>Tên Phim</th>
              <th>Thể Loại</th>
              <th>Độ Tuổi</th>
              <th>Thời Lượng</th>
              <th>Định Dạng</th>
              <th>Trạng Thái</th>
              <th>Hành Động</th>
            </tr>
          </thead>
          <tbody>
            ${this.moviesList.map(m => `
              <tr>
                <td><img src="${m.posterUrl}" class="table-poster" alt="${m.title}"></td>
                <td>
                  <strong>${m.title}</strong>
                  <div style="font-size: 11px; color: #64748b;">${m.originalTitle || ''}</div>
                </td>
                <td>${m.genre}</td>
                <td><span style="font-weight: 800; color: var(--cgv-red);">${m.ageRating}</span></td>
                <td>${m.duration} phút</td>
                <td>${m.formats ? m.formats.join(', ') : '2D'}</td>
                <td>
                  <span class="status-pill ${m.status === 'NOW_SHOWING' ? 'now-showing' : 'coming-soon'}">
                    ${m.status === 'NOW_SHOWING' ? 'ĐANG CHIẾU' : 'SẮP CHIẾU'}
                  </span>
                </td>
                <td>
                  <div class="table-actions">
                    <button class="btn-tbl-action" onclick="AdminApp.openEditMovie('${m.id}')">Sửa</button>
                    <button class="btn-tbl-action delete" onclick="AdminApp.deleteMovie('${m.id}')">Xóa</button>
                  </div>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;

    document.getElementById('btn-open-add-movie').addEventListener('click', () => {
      this.openEditMovie(null);
    });

    document.getElementById('search-movie-input').addEventListener('input', (e) => {
      const q = e.target.value.toLowerCase();
      const rows = document.querySelectorAll('#movies-table tbody tr');
      rows.forEach(r => {
        r.style.display = r.textContent.toLowerCase().includes(q) ? '' : 'none';
      });
    });
  },

  /* ==========================================================================
     3. CINEMAS VIEW
     ========================================================================== */
  renderCinemasView(container) {
    container.innerHTML = `
      <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px;">
        ${this.cinemasList.map(c => `
          <div class="dashboard-panel">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
              <div>
                <h3 style="font-size: 16px; font-weight: 800; color: #0f172a;">${c.name}</h3>
                <div style="font-size: 12px; color: #64748b; margin-top: 2px;">${c.address} (${c.city})</div>
              </div>
              <span class="status-pill now-showing">HOẠT ĐỘNG</span>
            </div>
            
            <div style="display: flex; gap: 20px; padding: 12px 0; border-top: 1px solid #e2e8f0; border-bottom: 1px solid #e2e8f0; margin-bottom: 14px; font-size: 13px;">
              <div><strong>Phòng chiếu:</strong> ${c.totalRooms || 8} phòng</div>
              <div><strong>Sức chứa:</strong> ${c.totalSeats || 1200} ghế</div>
            </div>

            <div style="display: flex; flex-wrap: wrap; gap: 6px;">
              ${(c.specialFormats || ['IMAX', 'GOLD CLASS', '4DX']).map(f => `
                <span style="font-size: 11px; font-weight: 800; background: #e2e8f0; padding: 4px 8px; border-radius: 4px;">${f}</span>
              `).join('')}
            </div>
          </div>
        `).join('')}
      </div>
    `;
  },

  /* ==========================================================================
     4. SHOWTIMES VIEW
     ========================================================================== */
  renderShowtimesView(container) {
    container.innerHTML = `
      <div class="table-controls">
        <div style="display: flex; gap: 12px;">
          <select id="filter-cinema-select" style="padding: 10px 14px; border: 1px solid var(--border-color); border-radius: 6px;">
            <option value="">Tất cả các rạp</option>
            ${this.cinemasList.map(c => `<option value="${c.name}">${c.name}</option>`).join('')}
          </select>
        </div>
        <button class="btn-add-new" id="btn-open-add-showtime">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          <span>Xếp Lịch Suất Chiếu</span>
        </button>
      </div>

      <div class="data-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>Giờ Chiếu</th>
              <th>Ngày</th>
              <th>Tên Phim</th>
              <th>Cụm Rạp</th>
              <th>Phòng</th>
              <th>Định Dạng</th>
              <th>Ghế Đã Bán</th>
              <th>Giá Thường / VIP</th>
              <th>Hành Động</th>
            </tr>
          </thead>
          <tbody>
            ${this.showtimesList.map(s => `
              <tr>
                <td><strong style="color: var(--cgv-red); font-size: 14px;">${s.time}</strong></td>
                <td>${s.date}</td>
                <td><strong>${s.movieTitle}</strong></td>
                <td>${s.cinemaName}</td>
                <td>${s.room}</td>
                <td><span class="status-pill paid">${s.format}</span></td>
                <td>
                  <strong>${s.occupiedSeats}/${s.totalSeats}</strong>
                  <div style="font-size: 11px; color: #64748b;">${Math.round((s.occupiedSeats/s.totalSeats)*100)}% lấp đầy</div>
                </td>
                <td>${new Intl.NumberFormat('vi-VN').format(s.priceRegular)} / ${new Intl.NumberFormat('vi-VN').format(s.priceVip)} đ</td>
                <td>
                  <button class="btn-tbl-action delete" onclick="AdminApp.deleteShowtime('${s.id}')">Hủy</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;

    document.getElementById('btn-open-add-showtime').addEventListener('click', () => {
      this.openAddShowtimeModal();
    });
  },

  /* ==========================================================================
     5. BOOKINGS VIEW
     ========================================================================== */
  renderBookingsView(container) {
    container.innerHTML = `
      <div class="table-controls">
        <div class="search-input-wrap">
          <input type="text" id="search-booking-input" placeholder="Tìm mã vé, khách hàng, số điện thoại...">
        </div>
      </div>

      <div class="data-table-wrap">
        <table class="admin-table" id="bookings-table">
          <thead>
            <tr>
              <th>Mã Đặt Vé</th>
              <th>Khách Hàng</th>
              <th>Số Điện Thoại</th>
              <th>Phim</th>
              <th>Suất & Rạp</th>
              <th>Ghế Ngồi</th>
              <th>Tổng Tiền</th>
              <th>Cổng Thanh Toán</th>
              <th>Trạng Thái</th>
            </tr>
          </thead>
          <tbody>
            ${this.bookingsList.map(b => `
              <tr>
                <td><strong style="color: var(--cgv-red);">${b.id}</strong></td>
                <td><strong>${b.customerName}</strong></td>
                <td>${b.phone}</td>
                <td>${b.movieTitle}</td>
                <td>
                  <div>${b.showtime}</div>
                  <div style="font-size: 11px; color: #64748b;">${b.cinemaName}</div>
                </td>
                <td><span style="font-weight: bold; color: #0284c7;">${b.seats}</span></td>
                <td><strong>${new Intl.NumberFormat('vi-VN').format(b.amount)} đ</strong></td>
                <td>${b.paymentMethod}</td>
                <td><span class="status-pill paid">${b.status}</span></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;

    document.getElementById('search-booking-input').addEventListener('input', (e) => {
      const q = e.target.value.toLowerCase();
      document.querySelectorAll('#bookings-table tbody tr').forEach(r => {
        r.style.display = r.textContent.toLowerCase().includes(q) ? '' : 'none';
      });
    });
  },

  /* ==========================================================================
     6. VOUCHERS VIEW
     ========================================================================== */
  renderVouchersView(container) {
    container.innerHTML = `
      <div class="table-controls">
        <h3 style="font-size: 15px; font-weight: bold;">Chương trình Khuyến Mãi & Voucher Giảm Giá</h3>
        <button class="btn-add-new" id="btn-open-add-voucher">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          <span>Tạo Mã Voucher</span>
        </button>
      </div>

      <div class="data-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>Mã Voucher</th>
              <th>Tên Chương Trình</th>
              <th>Mức Ưu Đãi</th>
              <th>Lượt Sử Dụng (Đã Dùng / Giới Hạn)</th>
              <th>Hạn Dùng</th>
              <th>Trạng Thái</th>
            </tr>
          </thead>
          <tbody>
            ${this.vouchersList.map(v => `
              <tr>
                <td><strong style="color: var(--cgv-red); font-size: 14px; letter-spacing: 1px;">${v.code}</strong></td>
                <td><strong>${v.name}</strong></td>
                <td>${new Intl.NumberFormat('vi-VN').format(v.discountValue)} đ</td>
                <td>
                  <strong>${v.usedCount.toLocaleString()} / ${v.usageLimit.toLocaleString()}</strong>
                  <div style="font-size: 11px; color: #64748b;">${Math.round((v.usedCount/v.usageLimit)*100)}% hạn mức</div>
                </td>
                <td>${v.expiryDate}</td>
                <td><span class="status-pill active">${v.status}</span></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;

    document.getElementById('btn-open-add-voucher').addEventListener('click', () => {
      this.openAddVoucherModal();
    });
  },

  /* ==========================================================================
     MODALS LOGIC
     ========================================================================== */
  setupModals() {
    this.movieModal = document.getElementById('modal-movie');
    this.showtimeModal = document.getElementById('modal-showtime');
    this.voucherModal = document.getElementById('modal-voucher');

    document.querySelectorAll('.modal-close-btn, .btn-cancel').forEach(b => {
      b.addEventListener('click', () => {
        document.querySelectorAll('.admin-modal-backdrop').forEach(m => m.classList.remove('active'));
      });
    });

    // Submit Movie Form
    document.getElementById('form-movie').addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('movie-id').value;
      const movieData = {
        id: id || undefined,
        title: document.getElementById('movie-title').value.trim(),
        originalTitle: document.getElementById('movie-original-title').value.trim(),
        genre: document.getElementById('movie-genre').value.trim(),
        duration: parseInt(document.getElementById('movie-duration').value, 10),
        ageRating: document.getElementById('movie-age-rating').value,
        status: document.getElementById('movie-status').value,
        formats: document.getElementById('movie-formats').value.split(',').map(s => s.trim()),
        posterUrl: document.getElementById('movie-poster').value.trim() || 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&q=80'
      };

      await AdminApi.saveMovie(movieData);
      this.movieModal.classList.remove('active');
      this.showToast(id ? "Đã cập nhật phim thành công!" : "Đã thêm phim mới thành công!");
      await this.loadAllData();
      this.renderCurrentView();
    });

    // Submit Showtime Form
    document.getElementById('form-showtime').addEventListener('submit', async (e) => {
      e.preventDefault();
      const stData = {
        movieId: document.getElementById('st-movie-select').value,
        movieTitle: document.getElementById('st-movie-select').selectedOptions[0].text,
        cinemaName: document.getElementById('st-cinema-select').value,
        room: document.getElementById('st-room').value,
        format: document.getElementById('st-format').value,
        date: document.getElementById('st-date').value,
        time: document.getElementById('st-time').value,
        priceRegular: parseInt(document.getElementById('st-price-regular').value, 10),
        priceVip: parseInt(document.getElementById('st-price-vip').value, 10)
      };

      await AdminApi.createShowtime(stData);
      this.showtimeModal.classList.remove('active');
      this.showToast("Đã xếp lịch suất chiếu mới thành công!");
      await this.loadAllData();
      this.renderCurrentView();
    });

    // Submit Voucher Form
    document.getElementById('form-voucher').addEventListener('submit', async (e) => {
      e.preventDefault();
      const vData = {
        code: document.getElementById('v-code').value.trim().toUpperCase(),
        name: document.getElementById('v-name').value.trim(),
        discountValue: parseInt(document.getElementById('v-value').value, 10),
        usageLimit: parseInt(document.getElementById('v-limit').value, 10),
        expiryDate: document.getElementById('v-expiry').value
      };

      await AdminApi.createVoucher(vData);
      this.voucherModal.classList.remove('active');
      this.showToast("Đã tạo mã voucher mới thành công!");
      await this.loadAllData();
      this.renderCurrentView();
    });
  },

  openEditMovie(id = null) {
    const titleEl = document.getElementById('movie-modal-title');
    const form = document.getElementById('form-movie');
    form.reset();

    if (id) {
      const movie = this.moviesList.find(m => m.id === id);
      if (!movie) return;
      titleEl.textContent = "Chỉnh Sửa Thông Tin Phim";
      document.getElementById('movie-id').value = movie.id;
      document.getElementById('movie-title').value = movie.title;
      document.getElementById('movie-original-title').value = movie.originalTitle || '';
      document.getElementById('movie-genre').value = movie.genre;
      document.getElementById('movie-duration').value = movie.duration;
      document.getElementById('movie-age-rating').value = movie.ageRating;
      document.getElementById('movie-status').value = movie.status;
      document.getElementById('movie-formats').value = movie.formats ? movie.formats.join(', ') : '2D';
      document.getElementById('movie-poster').value = movie.posterUrl || '';
    } else {
      titleEl.textContent = "Thêm Phim Mới Vào Hệ Thống";
      document.getElementById('movie-id').value = '';
    }

    this.movieModal.classList.add('active');
  },

  async deleteMovie(id) {
    if (!confirm("Bạn có chắc chắn muốn xóa phim này khỏi danh mục?")) return;
    await AdminApi.deleteMovie(id);
    this.showToast("Đã xóa phim thành công");
    await this.loadAllData();
    this.renderCurrentView();
  },

  openAddShowtimeModal() {
    const movieSel = document.getElementById('st-movie-select');
    const cinemaSel = document.getElementById('st-cinema-select');

    movieSel.innerHTML = this.moviesList.map(m => `<option value="${m.id}">${m.title}</option>`).join('');
    cinemaSel.innerHTML = this.cinemasList.map(c => `<option value="${c.name}">${c.name}</option>`).join('');

    const today = new Date().toISOString().split('T')[0];
    document.getElementById('st-date').value = today;

    this.showtimeModal.classList.add('active');
  },

  async deleteShowtime(id) {
    if (!confirm("Bạn có chắc chắn muốn hủy suất chiếu này?")) return;
    await AdminApi.deleteShowtime(id);
    this.showToast("Đã hủy suất chiếu");
    await this.loadAllData();
    this.renderCurrentView();
  },

  openAddVoucherModal() {
    document.getElementById('form-voucher').reset();
    this.voucherModal.classList.add('active');
  },

  switchTab(tab) {
    const menuItems = document.querySelectorAll('.sidebar-menu .menu-item');
    menuItems.forEach(m => {
      m.classList.toggle('active', m.dataset.tab === tab);
    });
    this.currentTab = tab;
    this.renderCurrentView();
  },

  showToast(message) {
    const existing = document.querySelector('.admin-toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = 'admin-toast';
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  }
};

document.addEventListener('DOMContentLoaded', () => {
  AdminApp.init();
});
