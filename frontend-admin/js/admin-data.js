/**
 * Admin Data Store cho CGV Cinemas Management Portal
 * Tự động đồng bộ với localStorage để lưu trữ các thao tác CRUD cục bộ
 */
const AdminData = {
  STORAGE_KEY: 'cgv_admin_store_v1',

  state: {
    movies: [],
    cinemas: [],
    showtimes: [],
    bookings: [],
    vouchers: [],
    stats: {
      todayRevenue: 185450000,
      todayTickets: 1420,
      activeShowtimes: 48,
      occupancyRate: 78.5
    }
  },

  init() {
    const saved = localStorage.getItem(this.STORAGE_KEY);
    if (saved) {
      try {
        this.state = JSON.parse(saved);
        return;
      } catch (e) {
        console.warn("Dữ liệu lưu trữ cũ không hợp lệ, tải lại dữ liệu mặc định");
      }
    }
    this.seedDefaultData();
    this.save();
  },

  save() {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.state));
  },

  seedDefaultData() {
    this.state.movies = [
      {
        id: "mov-001",
        title: "DUNE: HÀNH TINH CÁT - PHẦN HAI",
        originalTitle: "Dune: Part Two",
        status: "NOW_SHOWING",
        ageRating: "T16",
        duration: 166,
        releaseDate: "2024-03-01",
        genre: "Khoa Học Viễn Tưởng, Phiêu Lưu",
        director: "Denis Villeneuve",
        posterUrl: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400&q=80",
        formats: ["IMAX 2D", "2D Phụ Đề", "4DX"],
        ticketsSold: 18450,
        revenue: 2415000000
      },
      {
        id: "mov-002",
        title: "QUẬT MỘ TRÙNG MA",
        originalTitle: "Exhuma",
        status: "NOW_SHOWING",
        ageRating: "T18",
        duration: 134,
        releaseDate: "2024-03-15",
        genre: "Kinh Dị, Bí Ẩn, Giật Gân",
        director: "Jang Jae-hyun",
        posterUrl: "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=400&q=80",
        formats: ["2D Phụ Đề", "4DX"],
        ticketsSold: 14200,
        revenue: 1680000000
      },
      {
        id: "mov-003",
        title: "GODZILLA X KONG: ĐẾ CHẾ MỚI",
        originalTitle: "Godzilla x Kong: The New Empire",
        status: "NOW_SHOWING",
        ageRating: "T13",
        duration: 115,
        releaseDate: "2024-03-29",
        genre: "Hành Động, Viễn Tưởng",
        director: "Adam Wingard",
        posterUrl: "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=400&q=80",
        formats: ["IMAX 3D", "4DX 3D", "2D Phụ Đề"],
        ticketsSold: 11200,
        revenue: 1450000000
      },
      {
        id: "mov-004",
        title: "KUNG FU PANDA 4",
        originalTitle: "Kung Fu Panda 4",
        status: "NOW_SHOWING",
        ageRating: "P",
        duration: 94,
        releaseDate: "2024-03-08",
        genre: "Hoạt Hình, Hài Hước",
        director: "Mike Mitchell",
        posterUrl: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&q=80",
        formats: ["2D Lồng Tiếng", "2D Phụ Đề"],
        ticketsSold: 9600,
        revenue: 1050000000
      },
      {
        id: "mov-005",
        title: "DEADPOOL & WOLVERINE",
        originalTitle: "Deadpool & Wolverine",
        status: "COMING_SOON",
        ageRating: "T18",
        duration: 127,
        releaseDate: "2024-07-26",
        genre: "Hành Động, Hài Hước",
        director: "Shawn Levy",
        posterUrl: "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=400&q=80",
        formats: ["IMAX 3D", "4DX", "2D Phụ Đề"],
        ticketsSold: 0,
        revenue: 0
      }
    ];

    this.state.cinemas = [
      {
        id: "cin-001",
        name: "CGV Vincom Center Đồng Khởi",
        city: "TP. Hồ Chí Minh",
        address: "72 Lê Thánh Tôn, Bến Nghé, Quận 1",
        totalRooms: 7,
        totalSeats: 1050,
        specialFormats: ["GOLD CLASS", "PREMIUM", "SWEETBOX"]
      },
      {
        id: "cin-002",
        name: "CGV Landmark 81",
        city: "TP. Hồ Chí Minh",
        address: "772 Điện Biên Phủ, Phường 22, Bình Thạnh",
        totalRooms: 8,
        totalSeats: 1280,
        specialFormats: ["IMAX LASER", "GOLD CLASS"]
      },
      {
        id: "cin-003",
        name: "CGV Sư Vạn Hạnh",
        city: "TP. Hồ Chí Minh",
        address: "11 Sư Vạn Hạnh, Phường 12, Quận 10",
        totalRooms: 9,
        totalSeats: 1420,
        specialFormats: ["4DX", "SCREENX"]
      },
      {
        id: "cin-004",
        name: "CGV Vincom Center Bà Triệu",
        city: "Hà Nội",
        address: "191 Bà Triệu, Quận Hai Bà Trưng",
        totalRooms: 10,
        totalSeats: 1560,
        specialFormats: ["GOLD CLASS", "IMAX"]
      }
    ];

    this.state.showtimes = [
      {
        id: "st-101",
        movieId: "mov-001",
        movieTitle: "DUNE: HÀNH TINH CÁT - PHẦN HAI",
        cinemaId: "cin-002",
        cinemaName: "CGV Landmark 81",
        room: "Phòng chiếu 1 (IMAX)",
        format: "IMAX 2D",
        date: "2024-04-15",
        time: "09:30",
        priceRegular: 110000,
        priceVip: 130000,
        priceSweetbox: 260000,
        occupiedSeats: 72,
        totalSeats: 120
      },
      {
        id: "st-102",
        movieId: "mov-001",
        movieTitle: "DUNE: HÀNH TINH CÁT - PHẦN HAI",
        cinemaId: "cin-002",
        cinemaName: "CGV Landmark 81",
        room: "Phòng chiếu 1 (IMAX)",
        format: "IMAX 2D",
        date: "2024-04-15",
        time: "13:45",
        priceRegular: 120000,
        priceVip: 140000,
        priceSweetbox: 280000,
        occupiedSeats: 98,
        totalSeats: 120
      },
      {
        id: "st-103",
        movieId: "mov-002",
        movieTitle: "QUẬT MỘ TRÙNG MA",
        cinemaId: "cin-001",
        cinemaName: "CGV Vincom Đồng Khởi",
        room: "Phòng chiếu 3",
        format: "2D Phụ Đề",
        date: "2024-04-15",
        time: "19:00",
        priceRegular: 115000,
        priceVip: 135000,
        priceSweetbox: 270000,
        occupiedSeats: 112,
        totalSeats: 120
      }
    ];

    this.state.bookings = [
      {
        id: "BK-894102",
        customerName: "Nguyễn Văn Hưng",
        phone: "0912345678",
        movieTitle: "Dune: Part Two",
        cinemaName: "CGV Landmark 81",
        showtime: "13:45 - 15/04/2024",
        seats: "F5, F6 (VIP)",
        amount: 280000,
        paymentMethod: "VNPAY QR",
        status: "PAID",
        createdAt: "2024-04-15 11:20"
      },
      {
        id: "BK-894103",
        customerName: "Trần Thị Mai",
        phone: "0987654321",
        movieTitle: "Quật Mộ Trùng Ma",
        cinemaName: "CGV Vincom Đồng Khởi",
        showtime: "19:00 - 15/04/2024",
        seats: "E7, E8 (VIP)",
        amount: 270000,
        paymentMethod: "ZALOPAY",
        status: "PAID",
        createdAt: "2024-04-15 12:05"
      },
      {
        id: "BK-894104",
        customerName: "Lê Quốc Bảo",
        phone: "0909112233",
        movieTitle: "Godzilla x Kong",
        cinemaName: "CGV Sư Vạn Hạnh",
        showtime: "16:40 - 15/04/2024",
        seats: "J1-J2 (Sweetbox)",
        amount: 380000,
        paymentMethod: "MOMO",
        status: "PAID",
        createdAt: "2024-04-15 12:45"
      }
    ];

    this.state.vouchers = [
      {
        code: "CGVU22",
        name: "Ưu đãi sinh viên U22 giá 45K",
        discountType: "FIXED_PRICE",
        discountValue: 45000,
        usedCount: 4120,
        usageLimit: 10000,
        status: "ACTIVE",
        expiryDate: "2024-12-31"
      },
      {
        code: "HAPPYWED",
        name: "Thứ 4 vui vẻ đồng giá 60K",
        discountType: "FIXED_PRICE",
        discountValue: 60000,
        usedCount: 8900,
        usageLimit: 20000,
        status: "ACTIVE",
        expiryDate: "2024-12-31"
      },
      {
        code: "CGVVNPAY20",
        name: "Giảm 20.000đ khi thanh toán VNPAY",
        discountType: "DIRECT_DISCOUNT",
        discountValue: 20000,
        usedCount: 3450,
        usageLimit: 5000,
        status: "ACTIVE",
        expiryDate: "2024-06-30"
      }
    ];
  }
};

AdminData.init();
