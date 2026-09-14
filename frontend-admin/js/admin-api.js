/**
 * API Service cho CGV Admin Portal
 * Bảo mật: Không hardcode domain công khai.
 * Hỗ trợ cờ USE_MOCK để chuyển sang BE thật khi người dùng sẵn sàng.
 */
const AdminApi = {
  CONFIG: {
    BASE_URL: (typeof window !== 'undefined' && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'))
      ? 'http://localhost:8000'
      : '',
    USE_MOCK: true
  },

  async getDashboardStats() {
    if (this.CONFIG.USE_MOCK) {
      return AdminData.state.stats;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/bookings/stats/dashboard`);
    return this._handle(res);
  },

  async getMovies() {
    if (this.CONFIG.USE_MOCK) {
      return AdminData.state.movies;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/catalogs/admin/movies`);
    return this._handle(res);
  },

  async saveMovie(movie) {
    if (this.CONFIG.USE_MOCK) {
      if (movie.id) {
        const idx = AdminData.state.movies.findIndex(m => m.id === movie.id);
        if (idx !== -1) {
          AdminData.state.movies[idx] = { ...AdminData.state.movies[idx], ...movie };
        }
      } else {
        movie.id = 'mov-' + Date.now().toString().slice(-4);
        movie.ticketsSold = 0;
        movie.revenue = 0;
        AdminData.state.movies.unshift(movie);
      }
      AdminData.save();
      return movie;
    }
    const method = movie.id ? 'PUT' : 'POST';
    const url = movie.id 
      ? `${this.CONFIG.BASE_URL}/api/v1/catalogs/admin/movies/${movie.id}`
      : `${this.CONFIG.BASE_URL}/api/v1/catalogs/admin/movies`;
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(movie)
    });
    return this._handle(res);
  },

  async deleteMovie(id) {
    if (this.CONFIG.USE_MOCK) {
      AdminData.state.movies = AdminData.state.movies.filter(m => m.id !== id);
      AdminData.save();
      return true;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/catalogs/admin/movies/${id}`, { method: 'DELETE' });
    return this._handle(res);
  },

  async getCinemas() {
    if (this.CONFIG.USE_MOCK) {
      return AdminData.state.cinemas;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/catalogs/admin/cinemas`);
    return this._handle(res);
  },

  async getShowtimes() {
    if (this.CONFIG.USE_MOCK) {
      return AdminData.state.showtimes;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/catalogs/admin/showtimes`);
    return this._handle(res);
  },

  async createShowtime(data) {
    if (this.CONFIG.USE_MOCK) {
      data.id = 'st-' + Date.now().toString().slice(-4);
      data.occupiedSeats = 0;
      data.totalSeats = 120;
      AdminData.state.showtimes.unshift(data);
      AdminData.save();
      return data;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/catalogs/admin/showtimes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    return this._handle(res);
  },

  async deleteShowtime(id) {
    if (this.CONFIG.USE_MOCK) {
      AdminData.state.showtimes = AdminData.state.showtimes.filter(s => s.id !== id);
      AdminData.save();
      return true;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/catalogs/admin/showtimes/${id}`, { method: 'DELETE' });
    return this._handle(res);
  },

  async getBookings() {
    if (this.CONFIG.USE_MOCK) {
      return AdminData.state.bookings;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/bookings/admin/orders`);
    return this._handle(res);
  },

  async getVouchers() {
    if (this.CONFIG.USE_MOCK) {
      return AdminData.state.vouchers;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/marketings/admin/vouchers`);
    return this._handle(res);
  },

  async createVoucher(data) {
    if (this.CONFIG.USE_MOCK) {
      data.usedCount = 0;
      data.status = 'ACTIVE';
      AdminData.state.vouchers.unshift(data);
      AdminData.save();
      return data;
    }
    const res = await fetch(`${this.CONFIG.BASE_URL}/api/v1/marketings/admin/vouchers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    return this._handle(res);
  },

  async _handle(res) {
    if (!res.ok) throw new Error(`Lỗi kết nối Backend (${res.status})`);
    const json = await res.json();
    return json.data !== undefined ? json.data : json;
  }
};
