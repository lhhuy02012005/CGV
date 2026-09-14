/**
 * Mock Data phục vụ giao diện CGV Cinemas
 * Cấu trúc dữ liệu tương thích 100% với DTO của Backend CineMatrix Enterprise
 */
const MOCK_DATA = {
  movies: [
    {
      id: "mov-001",
      title: "DUNE: HÀNH TINH CÁT - PHẦN HAI",
      originalTitle: "Dune: Part Two",
      status: "NOW_SHOWING",
      ageRating: "T16",
      ageRatingDesc: "Phim được phổ biến đến người xem từ đủ 16 tuổi trở lên",
      duration: 166,
      releaseDate: "2024-03-01",
      genre: "Khoa Học Viễn Tưởng, Phiêu Lưu",
      director: "Denis Villeneuve",
      cast: "Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem",
      posterUrl: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
      backdropUrl: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&q=80",
      trailerUrl: "https://www.youtube.com/embed/Way9Dexny3w",
      formats: ["IMAX 2D", "2D Phụ Đề", "4DX"],
      synopsis: "Paul Atreides liên minh với Chani và người Fremen để trả thù những kẻ đã hủy hoại gia tộc mình, đối mặt với sự lựa chọn giữa tình yêu và số phận vũ trụ.",
      rating: 8.8,
      votes: 14200
    },
    {
      id: "mov-002",
      title: "QUẬT MỘ TRÙNG MA",
      originalTitle: "Exhuma",
      status: "NOW_SHOWING",
      ageRating: "T18",
      ageRatingDesc: "Phim được phổ biến đến người xem từ đủ 18 tuổi trở lên",
      duration: 134,
      releaseDate: "2024-03-15",
      genre: "Kinh Dị, Bí Ẩn, Giật Gân",
      director: "Jang Jae-hyun",
      cast: "Choi Min-sik, Kim Go-eun, Yoo Hae-jin, Lee Do-hyun",
      posterUrl: "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
      backdropUrl: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&q=80",
      trailerUrl: "https://www.youtube.com/embed/C28f9qE2R5Y",
      formats: ["2D Phụ Đề", "4DX"],
      synopsis: "Quá trình khai quật một ngôi mộ bí ẩn đem lại hậu quả kinh hoàng khi giải phóng một thực thể tà ác chôn giấu suốt hàng trăm năm.",
      rating: 8.5,
      votes: 11500
    },
    {
      id: "mov-003",
      title: "GODZILLA X KONG: ĐẾ CHẾ MỚI",
      originalTitle: "Godzilla x Kong: The New Empire",
      status: "NOW_SHOWING",
      ageRating: "T13",
      ageRatingDesc: "Phim được phổ biến đến người xem từ đủ 13 tuổi trở lên",
      duration: 115,
      releaseDate: "2024-03-29",
      genre: "Hành Động, Viễn Tưởng",
      director: "Adam Wingard",
      cast: "Rebecca Hall, Brian Tyree Henry, Dan Stevens",
      posterUrl: "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&q=80",
      backdropUrl: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1600&q=80",
      trailerUrl: "https://www.youtube.com/embed/lV1OOlGwExg",
      formats: ["IMAX 3D", "4DX 3D", "2D Phụ Đề", "2D Lồng Tiếng"],
      synopsis: "Godzilla và Kong phải liên thủ trước một hiểm họa khổng lồ chưa từng thấy ẩn sâu trong Trái Đất rỗng đe dọa sự tồn vong của loài người.",
      rating: 8.2,
      votes: 9800
    },
    {
      id: "mov-004",
      title: "KUNG FU PANDA 4",
      originalTitle: "Kung Fu Panda 4",
      status: "NOW_SHOWING",
      ageRating: "P",
      ageRatingDesc: "Phim được phép phổ biến rộng rãi đến người xem ở mọi độ tuổi",
      duration: 94,
      releaseDate: "2024-03-08",
      genre: "Hoạt Hình, Hài Hước, Võ Thuật",
      director: "Mike Mitchell",
      cast: "Jack Black, Awkwafina, Viola Davis, Dustin Hoffman",
      posterUrl: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
      backdropUrl: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1600&q=80",
      trailerUrl: "https://www.youtube.com/embed/_inKs4eeHiI",
      formats: ["2D Lồng Tiếng", "2D Phụ Đề"],
      synopsis: "Po chuẩn bị trở thành Thủ lĩnh Tinh thần của Thung lũng Bình Yên và phải tìm kiếm một Thần Long Đại Hiệp kế nhiệm.",
      rating: 7.9,
      votes: 8200
    },
    {
      id: "mov-005",
      title: "LẬT MẶT 7: MỘT ĐIỀU ƯỚC",
      originalTitle: "Face Off 7: One Wish",
      status: "NOW_SHOWING",
      ageRating: "K",
      ageRatingDesc: "Phim được phổ biến đến người xem dưới 13 tuổi với điều kiện xem cùng cha mẹ",
      duration: 138,
      releaseDate: "2024-04-26",
      genre: "Gia Đình, Tâm Lý, Tình Cảm",
      director: "Lý Hải",
      cast: "Thanh Hiền, Trương Minh Cường, Đinh Y Nhung, Quách Ngọc Tuyên",
      posterUrl: "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&q=80",
      backdropUrl: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1600&q=80",
      trailerUrl: "https://www.youtube.com/embed/7KkK4BqGzX4",
      formats: ["2D", "ScreenX"],
      synopsis: "Câu chuyện gia đình cảm động về người mẹ già 73 tuổi cùng 5 người con với những hoàn cảnh và số phận khác nhau.",
      rating: 8.9,
      votes: 18500
    },
    {
      id: "mov-006",
      title: "DEADPOOL & WOLVERINE",
      originalTitle: "Deadpool & Wolverine",
      status: "COMING_SOON",
      ageRating: "T18",
      ageRatingDesc: "Phim được phổ biến đến người xem từ đủ 18 tuổi trở lên",
      duration: 127,
      releaseDate: "2024-07-26",
      genre: "Hành Động, Hài Hước, Siêu Anh Hùng",
      director: "Shawn Levy",
      cast: "Ryan Reynolds, Hugh Jackman, Emma Corrin, Matthew Macfadyen",
      posterUrl: "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800&q=80",
      backdropUrl: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&q=80",
      trailerUrl: "https://www.youtube.com/embed/73_1biulkYk",
      formats: ["IMAX 3D", "4DX", "2D Phụ Đề"],
      synopsis: "Tổ chức Phương sai Thời gian lôi kéo Wade Wilson vào một nhiệm vụ mới cùng với một biến thể bất đắc dĩ của Wolverine.",
      rating: 9.1,
      votes: 21000
    },
    {
      id: "mov-007",
      title: "KẺ TRỘM MẶT TRĂNG 4",
      originalTitle: "Despicable Me 4",
      status: "COMING_SOON",
      ageRating: "P",
      ageRatingDesc: "Phim được phép phổ biến rộng rãi đến người xem ở mọi độ tuổi",
      duration: 95,
      releaseDate: "2024-07-05",
      genre: "Hoạt Hình, Hài Hước, Gia Đình",
      director: "Chris Renaud",
      cast: "Steve Carell, Kristen Wiig, Will Ferrell, Sofia Vergara",
      posterUrl: "https://images.unsplash.com/photo-1563089145-599997674d42?w=800&q=80",
      backdropUrl: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1600&q=80",
      trailerUrl: "https://www.youtube.com/embed/qQlr9-rF32E",
      formats: ["2D Lồng Tiếng", "2D Phụ Đề", "4DX"],
      synopsis: "Gru và gia đình chào đón thành viên mới Gru Jr., đồng thời phải trốn chạy khỏi kẻ thù nguy hiểm Maxime Le Mal vừa vượt ngục.",
      rating: 8.0,
      votes: 5400
    },
    {
      id: "mov-008",
      title: "JOKER: ĐIÊN CÓ ĐÔI",
      originalTitle: "Joker: Folie à Deux",
      status: "COMING_SOON",
      ageRating: "T18",
      ageRatingDesc: "Phim được phổ biến đến người xem từ đủ 18 tuổi trở lên",
      duration: 138,
      releaseDate: "2024-10-04",
      genre: "Tội Phạm, Nhạc Kịch, Tâm Lý",
      director: "Todd Phillips",
      cast: "Joaquin Phoenix, Lady Gaga, Zazie Beetz, Brendan Gleeson",
      posterUrl: "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=800&q=80",
      backdropUrl: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1600&q=80",
      trailerUrl: "https://www.youtube.com/embed/_OKAwz2NiOI",
      formats: ["IMAX 2D", "2D Phụ Đề"],
      synopsis: "Arthur Fleck bị giam giữ tại Viện tâm thần Arkham trong lúc chờ xét xử cho những tội ác của Joker, nơi anh gặp gỡ tình yêu định mệnh Harley Quinn.",
      rating: 8.7,
      votes: 16800
    }
  ],

  cinemas: [
    {
      id: "cin-001",
      name: "CGV Vincom Center Đồng Khởi",
      city: "Hồ Chí Minh",
      district: "Quận 1",
      address: "Tầng 3, TTTM Vincom Center, 72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP. Hồ Chí Minh",
      specialRooms: ["GOLD CLASS", "PREMIUM", "SWEETBOX"]
    },
    {
      id: "cin-002",
      name: "CGV Landmark 81",
      city: "Hồ Chí Minh",
      district: "Bình Thạnh",
      address: "Tầng B1 , TTTM Vincom Center Landmark 81, 772 Điện Biên Phủ, Phường 22, Bình Thạnh",
      specialRooms: ["IMAX LASER", "GOLD CLASS"]
    },
    {
      id: "cin-003",
      name: "CGV Sư Vạn Hạnh",
      city: "Hồ Chí Minh",
      district: "Quận 10",
      address: "Tầng 6, Vạn Hạnh Mall, 11 Sư Vạn Hạnh, Phường 12, Quận 10",
      specialRooms: ["4DX", "SCREENX"]
    },
    {
      id: "cin-004",
      name: "CGV Vincom Center Bà Triệu",
      city: "Hà Nội",
      district: "Hai Bà Trưng",
      address: "Tầng 6, Vincom Center Hà Nội, 191 Bà Triệu, Quận Hai Bà Trưng",
      specialRooms: ["GOLD CLASS", "IMAX"]
    },
    {
      id: "cin-005",
      name: "CGV Vincom Metropolis Liễu Giai",
      city: "Hà Nội",
      district: "Ba Đình",
      address: "Tầng M3, TTTM Vincom Center Metropolis, 29 Liễu Giai, Quận Ba Đình",
      specialRooms: ["IMAX LASER", "L'AMOUR"]
    },
    {
      id: "cin-006",
      name: "CGV Vĩnh Trung Plaza",
      city: "Đà Nẵng",
      district: "Thanh Khê",
      address: "255-257 Hùng Vương, Phường Vĩnh Trung, Quận Thanh Khê, Đà Nẵng",
      specialRooms: ["GOLD CLASS"]
    }
  ],

  showtimes: [
    {
      id: "st-001",
      movieId: "mov-001",
      cinemaId: "cin-002",
      date: "2024-04-15",
      time: "09:30",
      format: "IMAX 2D",
      auditorium: "Phòng chiếu 1 (IMAX)",
      priceRegular: 110000,
      priceVip: 130000,
      priceSweetbox: 260000
    },
    {
      id: "st-002",
      movieId: "mov-001",
      cinemaId: "cin-002",
      date: "2024-04-15",
      time: "13:45",
      format: "IMAX 2D",
      auditorium: "Phòng chiếu 1 (IMAX)",
      priceRegular: 120000,
      priceVip: 140000,
      priceSweetbox: 280000
    },
    {
      id: "st-003",
      movieId: "mov-001",
      cinemaId: "cin-002",
      date: "2024-04-15",
      time: "17:15",
      format: "IMAX 2D",
      auditorium: "Phòng chiếu 1 (IMAX)",
      priceRegular: 140000,
      priceVip: 160000,
      priceSweetbox: 320000
    },
    {
      id: "st-004",
      movieId: "mov-001",
      cinemaId: "cin-002",
      date: "2024-04-15",
      time: "20:30",
      format: "IMAX 2D",
      auditorium: "Phòng chiếu 1 (IMAX)",
      priceRegular: 150000,
      priceVip: 170000,
      priceSweetbox: 340000
    },
    {
      id: "st-005",
      movieId: "mov-002",
      cinemaId: "cin-001",
      date: "2024-04-15",
      time: "10:00",
      format: "2D Phụ Đề",
      auditorium: "Phòng chiếu 3",
      priceRegular: 95000,
      priceVip: 115000,
      priceSweetbox: 230000
    },
    {
      id: "st-006",
      movieId: "mov-002",
      cinemaId: "cin-001",
      date: "2024-04-15",
      time: "14:20",
      format: "2D Phụ Đề",
      auditorium: "Phòng chiếu 3",
      priceRegular: 105000,
      priceVip: 125000,
      priceSweetbox: 250000
    },
    {
      id: "st-007",
      movieId: "mov-002",
      cinemaId: "cin-001",
      date: "2024-04-15",
      time: "19:00",
      format: "2D Phụ Đề",
      auditorium: "Phòng chiếu 3",
      priceRegular: 115000,
      priceVip: 135000,
      priceSweetbox: 270000
    },
    {
      id: "st-008",
      movieId: "mov-003",
      cinemaId: "cin-003",
      date: "2024-04-15",
      time: "11:15",
      format: "4DX 3D",
      auditorium: "Phòng chiếu 4DX",
      priceRegular: 160000,
      priceVip: 180000,
      priceSweetbox: 360000
    },
    {
      id: "st-009",
      movieId: "mov-003",
      cinemaId: "cin-003",
      date: "2024-04-15",
      time: "16:40",
      format: "4DX 3D",
      auditorium: "Phòng chiếu 4DX",
      priceRegular: 170000,
      priceVip: 190000,
      priceSweetbox: 380000
    },
    {
      id: "st-010",
      movieId: "mov-005",
      cinemaId: "cin-001",
      date: "2024-04-15",
      time: "18:30",
      format: "2D",
      auditorium: "Phòng chiếu 2",
      priceRegular: 105000,
      priceVip: 125000,
      priceSweetbox: 250000
    }
  ],

  promotions: [
    {
      id: "promo-001",
      title: "U22 - VÉ XEM PHIM 45K",
      subtitle: "Áp dụng từ Thứ 2 đến Thứ 6 cho thành viên U22 CGV",
      badge: "HOT NHẤT",
      imageUrl: "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=600&q=80",
      validUntil: "31/12/2024"
    },
    {
      id: "promo-002",
      title: "HAPPY WEDNESDAY - THỨ 4 VUI VẺ",
      subtitle: "Giá vé ưu đãi chỉ từ 65.000đ cho mọi suất chiếu thứ 4",
      badge: "HẰNG TUẦN",
      imageUrl: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&q=80",
      validUntil: "Vô thời hạn"
    },
    {
      id: "promo-003",
      title: "THẺ QUÀ TẶNG CGV GIFT CARD",
      subtitle: "Món quà tuyệt vời dành tặng người thân và bạn bè",
      badge: "ĐẶC BIỆT",
      imageUrl: "https://images.unsplash.com/photo-1549465220-1a8b9238cd48?w=600&q=80",
      validUntil: "31/12/2024"
    },
    {
      id: "promo-004",
      title: "GIẢM 20K KHI THANH TOÁN VNPAY",
      subtitle: "Nhập mã CGVVNPAY giảm ngay 20.000đ từ hóa đơn 100K",
      badge: "ĐỐI TÁC",
      imageUrl: "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=600&q=80",
      validUntil: "30/06/2024"
    }
  ],

  specialFormats: [
    {
      name: "IMAX with Laser",
      tagline: "Trải Nghiệm Điện Ảnh Đỉnh Cao",
      desc: "Hình ảnh laser 4K siêu sắc nét, độ tương phản cực đại và hệ thống âm thanh 12 kênh thế hệ mới.",
      bg: "linear-gradient(135deg, #0f2027, #203a43, #2c5364)"
    },
    {
      name: "4DX",
      tagline: "Đánh Thức Mọi Giác Quan",
      desc: "Chuyển động đa chiều kết hợp hiệu ứng môi trường: gió, mưa, sương mù, chớp sáng và mùi hương.",
      bg: "linear-gradient(135deg, #434343, #000000)"
    },
    {
      name: "ScreenX",
      tagline: "Toàn Cảnh 270 Độ",
      desc: "Mở rộng góc nhìn sang hai bên vách tường rạp, bao trọn toàn bộ tầm nhìn của khán giả.",
      bg: "linear-gradient(135deg, #141E30, #243B55)"
    },
    {
      name: "GOLD CLASS",
      tagline: "Đẳng Cấp Thượng Lưu",
      desc: "Ghế bọc da êm ái điều khiển điện tử, phục vụ trà và cà phê thượng hạng miễn phí tại chỗ.",
      bg: "linear-gradient(135deg, #3E2723, #1A0C08)"
    },
    {
      name: "L'AMOUR",
      tagline: "Ngọt Ngào Tình Nhân",
      desc: "Phòng chiếu giường nằm êm ái sang trọng, không gian riêng tư lãng mạn cùng đồ uống cao cấp.",
      bg: "linear-gradient(135deg, #4A148C, #880E4F)"
    }
  ]
};
