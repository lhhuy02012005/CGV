/**
 * Main Application Logic cho CGV Cinemas User Portal (Phiên bản Pixel-Perfect)
 */
const App = {
  currentSlideIndex: 0,
  sliderInterval: null,
  allMovies: [],
  cinemas: [],

  async init() {
    // 1. Khởi tạo modals
    BookingModal.init();
    AuthModal.init();
    this._initTrailerModal();

    // 2. Tải dữ liệu ban đầu
    await this.loadData();

    // 3. Render các thành phần
    this.renderSlider();
    this.renderMovies('NOW_SHOWING');
    this.renderEvents();

    // 4. Lắng nghe sự kiện
    this.setupEventListeners();
  },

  async loadData() {
    try {
      this.allMovies = await ApiService.getMovies();
      this.cinemas = await ApiService.getCinemas();
    } catch (e) {
      console.error("Lỗi tải dữ liệu ban đầu:", e);
    }
  },

  /* ==========================================================================
     HERO BANNER SLIDER (980px)
     ========================================================================== */
  renderSlider() {
    const track = document.getElementById('slider-track');
    const dotsContainer = document.getElementById('slider-dots');
    if (!track || this.allMovies.length === 0) return;

    track.innerHTML = '';
    dotsContainer.innerHTML = '';

    const featuredMovies = this.allMovies.slice(0, 4);

    featuredMovies.forEach((movie, index) => {
      const slide = document.createElement('div');
      slide.className = 'hero-slide';
      slide.style.backgroundImage = `url('${movie.backdropUrl || movie.posterUrl}')`;

      slide.addEventListener('click', () => {
        BookingModal.open(movie);
      });

      track.appendChild(slide);

      const dot = document.createElement('div');
      dot.className = `slider-dot-btn ${index === 0 ? 'active' : ''}`;
      dot.addEventListener('click', (e) => {
        e.stopPropagation();
        this.goToSlide(index);
      });
      dotsContainer.appendChild(dot);
    });

    this.startSliderTimer();
  },

  goToSlide(index) {
    const track = document.getElementById('slider-track');
    const dots = document.querySelectorAll('.slider-dot-btn');
    this.currentSlideIndex = index;
    track.style.transform = `translateX(-${index * 100}%)`;

    dots.forEach((d, i) => {
      d.classList.toggle('active', i === index);
    });
  },

  nextSlide() {
    const total = 4;
    const next = (this.currentSlideIndex + 1) % total;
    this.goToSlide(next);
  },

  prevSlide() {
    const total = 4;
    const prev = (this.currentSlideIndex - 1 + total) % total;
    this.goToSlide(prev);
  },

  startSliderTimer() {
    clearInterval(this.sliderInterval);
    this.sliderInterval = setInterval(() => this.nextSlide(), 5000);
  },

  /* ==========================================================================
     MOVIE SELECTION GRID (CHUẨN CGV)
     ========================================================================== */
  renderMovies(status = 'NOW_SHOWING') {
    const grid = document.getElementById('movie-grid');
    if (!grid) return;

    grid.innerHTML = '';
    const filtered = this.allMovies.filter(m => m.status === status);

    filtered.forEach(movie => {
      const card = document.createElement('div');
      card.className = 'cgv-movie-card';

      const ageLower = movie.ageRating.toLowerCase();
      const hasPromo = movie.id === 'mov-002' || movie.id === 'mov-005';

      card.innerHTML = `
        <div class="poster-box">
          <span class="age-badge ${ageLower}">${movie.ageRating}</span>
          ${hasPromo ? '<span class="promo-ribbon">59 000<br>VND</span>' : ''}
          <img src="${movie.posterUrl}" alt="${movie.title}" loading="lazy">
        </div>
        <div class="movie-card-body">
          <div class="movie-card-title" title="${movie.title}">${movie.title}</div>
          <div class="movie-card-actions">
            <button class="btn-card-detail" data-trailer-url="${movie.trailerUrl}">Xem Chi Tiết</button>
            <button class="btn-card-buy" data-movie-id="${movie.id}">🎟️ MUA VÉ</button>
          </div>
        </div>
      `;

      grid.appendChild(card);
    });

    // Sự kiện nút mua vé
    grid.querySelectorAll('.btn-card-buy').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.currentTarget.dataset.movieId;
        const movie = this.allMovies.find(m => m.id === id);
        if (movie) BookingModal.open(movie);
      });
    });

    // Sự kiện nút xem trailer
    grid.querySelectorAll('.btn-card-detail').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const url = e.currentTarget.dataset.trailerUrl;
        this.openTrailer(url);
      });
    });
  },

  /* ==========================================================================
     EVENTS ROW
     ========================================================================== */
  renderEvents() {
    const grid = document.getElementById('events-grid');
    if (!grid) return;

    grid.innerHTML = '';
    MOCK_DATA.promotions.forEach(p => {
      const card = document.createElement('div');
      card.className = 'event-banner-card';
      card.innerHTML = `
        <img src="${p.imageUrl}" alt="${p.title}" loading="lazy">
      `;
      card.addEventListener('click', () => {
        App.showToast(`Chương trình: ${p.title} (${p.subtitle})`);
      });
      grid.appendChild(card);
    });
  },

  /* ==========================================================================
     TRAILER MODAL
     ========================================================================== */
  _initTrailerModal() {
    this.trailerModal = document.getElementById('trailer-modal');
    this.trailerFrame = document.getElementById('trailer-iframe');
    const closeBtn = document.getElementById('close-trailer-modal');

    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.closeTrailer());
    }
    if (this.trailerModal) {
      this.trailerModal.addEventListener('click', (e) => {
        if (e.target === this.trailerModal) this.closeTrailer();
      });
    }
  },

  openTrailer(url) {
    if (!url || !this.trailerModal) return;
    this.trailerFrame.src = url + "?autoplay=1";
    this.trailerModal.classList.add('active');
    document.body.style.overflow = 'hidden';
  },

  closeTrailer() {
    if (!this.trailerModal) return;
    this.trailerModal.classList.remove('active');
    this.trailerFrame.src = '';
    document.body.style.overflow = '';
  },

  /* ==========================================================================
     GLOBAL EVENTS & TOAST
     ========================================================================== */
  setupEventListeners() {
    // Tabs Phim đang chiếu / Sắp chiếu
    const tabNow = document.getElementById('tab-now-showing');
    const tabSoon = document.getElementById('tab-coming-soon');

    if (tabNow && tabSoon) {
      tabNow.addEventListener('click', () => {
        tabNow.classList.add('active');
        tabSoon.classList.remove('active');
        this.renderMovies('NOW_SHOWING');
      });

      tabSoon.addEventListener('click', () => {
        tabSoon.classList.add('active');
        tabNow.classList.remove('active');
        this.renderMovies('COMING_SOON');
      });
    }

    // Nút điều khiển slider
    const prevBtn = document.getElementById('slider-prev');
    const nextBtn = document.getElementById('slider-next');
    if (prevBtn) prevBtn.addEventListener('click', () => this.prevSlide());
    if (nextBtn) nextBtn.addEventListener('click', () => this.nextSlide());

    // Nút "MUA VÉ NGAY" trên Header
    const btnBuyTicket = document.getElementById('btn-header-buy-ticket');
    if (btnBuyTicket) {
      btnBuyTicket.addEventListener('click', () => {
        const target = document.getElementById('movies');
        if (target) target.scrollIntoView({ behavior: 'smooth' });
      });
    }
  },

  showToast(message) {
    const existing = document.querySelector('.cgv-toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = 'cgv-toast';
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.4s ease';
      setTimeout(() => toast.remove(), 400);
    }, 3200);
  }
};

document.addEventListener('DOMContentLoaded', () => {
  App.init();
});
