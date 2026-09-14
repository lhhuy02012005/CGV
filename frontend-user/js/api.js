/**
 * API Service Layer cho CGV User Portal
 * Bảo mật: Không hardcode domain công khai.
 * Hỗ trợ chuyển đổi nhanh giữa Mock Data và Backend thật qua cờ USE_MOCK.
 */
const API_CONFIG = {
  // Tự động nhận diện host nội bộ, không phụ thuộc vào bất kỳ tên miền cố định nào
  BASE_URL: (typeof window !== 'undefined' && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'))
    ? 'http://localhost:8000'
    : '',
  USE_MOCK: true, // Chuyển thành false khi người dùng bật Backend Docker cổng 8000
  TIMEOUT: 8000
};

const ApiService = {
  /**
   * Lấy danh sách phim theo trạng thái: NOW_SHOWING hoặc COMING_SOON
   */
  async getMovies(status = null) {
    if (API_CONFIG.USE_MOCK) {
      await this._simulateLatency();
      if (!status) return MOCK_DATA.movies;
      return MOCK_DATA.movies.filter(m => m.status === status);
    }
    const query = status ? `?status=${encodeURIComponent(status)}` : '';
    const res = await fetch(`${API_CONFIG.BASE_URL}/api/v1/catalogs/movies${query}`);
    return this._handleResponse(res);
  },

  /**
   * Lấy chi tiết một phim
   */
  async getMovieById(id) {
    if (API_CONFIG.USE_MOCK) {
      await this._simulateLatency();
      return MOCK_DATA.movies.find(m => m.id === id) || null;
    }
    const res = await fetch(`${API_CONFIG.BASE_URL}/api/v1/catalogs/movies/${encodeURIComponent(id)}`);
    return this._handleResponse(res);
  },

  /**
   * Lấy danh sách rạp chiếu
   */
  async getCinemas() {
    if (API_CONFIG.USE_MOCK) {
      await this._simulateLatency();
      return MOCK_DATA.cinemas;
    }
    const res = await fetch(`${API_CONFIG.BASE_URL}/api/v1/catalogs/cinemas`);
    return this._handleResponse(res);
  },

  /**
   * Lấy lịch chiếu theo phim, rạp hoặc ngày
   */
  async getShowtimes(movieId = null, cinemaId = null, date = null) {
    if (API_CONFIG.USE_MOCK) {
      await this._simulateLatency();
      return MOCK_DATA.showtimes.filter(st => {
        if (movieId && st.movieId !== movieId) return false;
        if (cinemaId && st.cinemaId !== cinemaId) return false;
        if (date && st.date !== date) return false;
        return true;
      });
    }
    const params = new URLSearchParams();
    if (movieId) params.append('movieId', movieId);
    if (cinemaId) params.append('cinemaId', cinemaId);
    if (date) params.append('date', date);
    const res = await fetch(`${API_CONFIG.BASE_URL}/api/v1/catalogs/showtimes?${params.toString()}`);
    return this._handleResponse(res);
  },

  /**
   * Lấy danh sách khuyến mãi / sự kiện
   */
  async getPromotions() {
    if (API_CONFIG.USE_MOCK) {
      await this._simulateLatency();
      return MOCK_DATA.promotions;
    }
    const res = await fetch(`${API_CONFIG.BASE_URL}/api/v1/marketings/promotions`);
    return this._handleResponse(res);
  },

  /**
   * Đăng nhập (Khớp endpoint identityservice /api/v1/auth/login)
   */
  async login(username, password) {
    if (API_CONFIG.USE_MOCK) {
      await this._simulateLatency(500);
      if (!username || !password) throw new Error("Vui lòng điền đầy đủ tài khoản và mật khẩu");
      const mockUser = {
        token: "mock-jwt-token-cgv-" + Date.now(),
        user: {
          id: "u-999",
          username: username,
          fullName: "Khách Hàng CGV VIP",
          membershipTier: "VIP",
          points: 450
        }
      };
      localStorage.setItem('cgv_token', mockUser.token);
      localStorage.setItem('cgv_user', JSON.stringify(mockUser.user));
      return mockUser;
    }
    const res = await fetch(`${API_CONFIG.BASE_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await this._handleResponse(res);
    if (data && data.token) {
      localStorage.setItem('cgv_token', data.token);
      localStorage.setItem('cgv_user', JSON.stringify(data.user || {}));
    }
    return data;
  },

  /**
   * Đăng ký tài khoản (Khớp endpoint identityservice /api/v1/auth/register)
   */
  async register(registerData) {
    if (API_CONFIG.USE_MOCK) {
      await this._simulateLatency(600);
      return { success: true, message: "Đăng ký tài khoản CGV thành công!" };
    }
    const res = await fetch(`${API_CONFIG.BASE_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(registerData)
    });
    return this._handleResponse(res);
  },

  /**
   * Giữ ghế tạm thời (Holding Lock - 10 phút)
   */
  async holdSeats(showtimeId, seatCodes) {
    if (API_CONFIG.USE_MOCK) {
      await this._simulateLatency(300);
      return {
        holdId: "hold-" + Date.now(),
        expiresInSeconds: 600, // 10 phút
        seats: seatCodes
      };
    }
    const token = localStorage.getItem('cgv_token');
    const res = await fetch(`${API_CONFIG.BASE_URL}/api/v1/bookings/hold-seats`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify({ showtimeId, seatCodes })
    });
    return this._handleResponse(res);
  },

  _simulateLatency(ms = 150) {
    return new Promise(resolve => setTimeout(resolve, ms));
  },

  async _handleResponse(res) {
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: `HTTP Error ${res.status}` }));
      throw new Error(err.message || `Lỗi kết nối Backend (${res.status})`);
    }
    const json = await res.json();
    return json.data !== undefined ? json.data : json;
  }
};
