/**
 * Booking Modal & Interactive Seat Map Component
 * Quản lý chọn suất chiếu, sơ đồ ghế, tính tiền và đồng hồ giữ ghế (Redis TTL 10m)
 */
const BookingModal = {
  currentMovie: null,
  currentShowtime: null,
  selectedSeats: [],
  timerInterval: null,
  timeLeft: 600, // 10 phút

  init() {
    this.modalEl = document.getElementById('booking-modal');
    this.closeBtn = document.getElementById('close-booking-modal');
    this.screenWrap = document.getElementById('seat-grid-container');
    this.selectedSeatsEl = document.getElementById('selected-seats-text');
    this.totalPriceEl = document.getElementById('total-price-text');
    this.timerEl = document.getElementById('holding-timer');
    this.btnCheckout = document.getElementById('btn-checkout');

    this.closeBtn.addEventListener('click', () => this.close());
    this.modalEl.addEventListener('click', (e) => {
      if (e.target === this.modalEl) this.close();
    });

    this.btnCheckout.addEventListener('click', () => this.handleCheckout());
  },

  /**
   * Mở modal đặt vé
   * @param {Object} movie
   * @param {Object} showtime (tùy chọn)
   */
  async open(movie, showtime = null) {
    this.currentMovie = movie;
    this.selectedSeats = [];
    this.timeLeft = 600;

    document.getElementById('booking-movie-title').textContent = movie.title;
    document.getElementById('booking-movie-meta').textContent = 
      `${movie.ageRating} • ${movie.duration} phút • ${movie.genre}`;

    // Lấy suất chiếu nếu chưa có
    if (!showtime) {
      const showtimes = await ApiService.getShowtimes(movie.id);
      this.currentShowtime = showtimes.length > 0 ? showtimes[0] : null;
    } else {
      this.currentShowtime = showtime;
    }

    this._renderShowtimeDetails();
    this._renderSeatMap();
    this._updateSummary();
    this._startTimer();

    this.modalEl.classList.add('active');
    document.body.style.overflow = 'hidden';
  },

  close() {
    this.modalEl.classList.remove('active');
    document.body.style.overflow = '';
    clearInterval(this.timerInterval);
  },

  _renderShowtimeDetails() {
    const stEl = document.getElementById('booking-showtime-info');
    if (!this.currentShowtime) {
      stEl.textContent = "Hôm nay | Suất chiếu đặc biệt";
      return;
    }
    const cinemas = MOCK_DATA.cinemas;
    const cinema = cinemas.find(c => c.id === this.currentShowtime.cinemaId) || { name: "CGV Vincom Đồng Khởi" };
    stEl.innerHTML = `<strong>${cinema.name}</strong> • ${this.currentShowtime.auditorium} • Suất <strong>${this.currentShowtime.time}</strong> • Định dạng <strong>${this.currentShowtime.format}</strong>`;
  },

  _renderSeatMap() {
    this.screenWrap.innerHTML = '';
    const rows = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J'];
    const cols = 12;

    // Danh sách ghế đã bán ngẫu nhiên để mô phỏng rạp thực tế
    const occupiedSeats = new Set(['C5', 'C6', 'E7', 'E8', 'F6', 'F7', 'G4', 'G5', 'H8', 'H9']);

    rows.forEach(row => {
      const rowDiv = document.createElement('div');
      rowDiv.className = 'seat-row';

      const label = document.createElement('span');
      label.className = 'seat-row-label';
      label.textContent = row;
      rowDiv.appendChild(label);

      if (row === 'J') {
        // Hàng J là ghế Sweetbox đôi (6 cặp)
        for (let col = 1; col <= 6; col++) {
          const seatCode = `J${col * 2 - 1}-J${col * 2}`;
          const isOccupied = occupiedSeats.has(`J${col}`);

          const seat = document.createElement('div');
          seat.className = `seat-item sweetbox ${isOccupied ? 'occupied' : ''}`;
          seat.textContent = seatCode;
          seat.dataset.code = seatCode;
          seat.dataset.type = 'sweetbox';
          seat.dataset.price = this.currentShowtime ? this.currentShowtime.priceSweetbox : 250000;

          if (!isOccupied) {
            seat.addEventListener('click', () => this._toggleSeat(seat));
          }
          rowDiv.appendChild(seat);
        }
      } else {
        // Hàng thường và VIP
        for (let col = 1; col <= cols; col++) {
          const seatCode = `${row}${col}`;
          const isOccupied = occupiedSeats.has(seatCode);
          const isVip = ['E', 'F', 'G', 'H'].includes(row) && (col >= 3 && col <= 10);

          const seat = document.createElement('div');
          seat.className = `seat-item ${isVip ? 'vip' : 'standard'} ${isOccupied ? 'occupied' : ''}`;
          seat.textContent = col;
          seat.dataset.code = seatCode;
          seat.dataset.type = isVip ? 'vip' : 'standard';
          seat.dataset.price = isVip 
            ? (this.currentShowtime ? this.currentShowtime.priceVip : 125000)
            : (this.currentShowtime ? this.currentShowtime.priceRegular : 105000);

          if (!isOccupied) {
            seat.addEventListener('click', () => this._toggleSeat(seat));
          }
          rowDiv.appendChild(seat);
        }
      }

      this.screenWrap.appendChild(rowDiv);
    });
  },

  _toggleSeat(seatEl) {
    const code = seatEl.dataset.code;
    const price = parseInt(seatEl.dataset.price, 10);
    const type = seatEl.dataset.type;

    const existingIndex = this.selectedSeats.findIndex(s => s.code === code);
    if (existingIndex > -1) {
      this.selectedSeats.splice(existingIndex, 1);
      seatEl.classList.remove('selected');
    } else {
      if (this.selectedSeats.length >= 8) {
        App.showToast("Mỗi lần đặt chỉ được chọn tối đa 8 ghế");
        return;
      }
      this.selectedSeats.push({ code, price, type });
      seatEl.classList.add('selected');
    }

    this._updateSummary();
  },

  _updateSummary() {
    if (this.selectedSeats.length === 0) {
      this.selectedSeatsEl.textContent = "Chưa chọn ghế";
      this.totalPriceEl.textContent = "0 đ";
      this.btnCheckout.disabled = true;
      this.btnCheckout.style.opacity = '0.5';
      return;
    }

    const codes = this.selectedSeats.map(s => s.code).join(', ');
    const total = this.selectedSeats.reduce((sum, s) => sum + s.price, 0);

    this.selectedSeatsEl.textContent = codes;
    this.totalPriceEl.textContent = new Intl.NumberFormat('vi-VN').format(total) + " đ";
    this.btnCheckout.disabled = false;
    this.btnCheckout.style.opacity = '1';
  },

  _startTimer() {
    clearInterval(this.timerInterval);
    const updateDisplay = () => {
      const minutes = Math.floor(this.timeLeft / 60);
      const seconds = this.timeLeft % 60;
      this.timerEl.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
      if (this.timeLeft <= 0) {
        clearInterval(this.timerInterval);
        App.showToast("Hết thời gian giữ ghế! Vui lòng chọn lại.");
        this.close();
      }
      this.timeLeft--;
    };
    updateDisplay();
    this.timerInterval = setInterval(updateDisplay, 1000);
  },

  async handleCheckout() {
    if (this.selectedSeats.length === 0) return;

    try {
      this.btnCheckout.textContent = "Đang giữ ghế...";
      this.btnCheckout.disabled = true;

      const seatCodes = this.selectedSeats.map(s => s.code);
      const res = await ApiService.holdSeats(this.currentShowtime ? this.currentShowtime.id : 'st-001', seatCodes);

      App.showToast(`Giữ ghế thành công (${seatCodes.join(', ')}). Sẵn sàng chuyển sang cổng thanh toán!`);
      setTimeout(() => {
        this.close();
      }, 1800);
    } catch (err) {
      App.showToast(err.message || "Không thể giữ ghế, vui lòng thử lại");
    } finally {
      this.btnCheckout.textContent = "Tiến Hành Thanh Toán";
      this.btnCheckout.disabled = false;
    }
  }
};
