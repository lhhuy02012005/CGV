-- ==============================================================================
-- CATALOG SERVICE - SCHEMA MIGRATION V1
-- ==============================================================================
CREATE TABLE IF NOT EXISTS regions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cinemas (
    id UUID PRIMARY KEY,
    region_id INTEGER NOT NULL REFERENCES regions(id),
    name VARCHAR(255) NOT NULL UNIQUE,
    address VARCHAR(500),
    phone VARCHAR(20),
    opening_hours VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'TEMPORARILY_CLOSED', 'PERMANENTLY_CLOSED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cinema_amenities (
    cinema_id UUID NOT NULL REFERENCES cinemas(id) ON DELETE CASCADE,
    amenity VARCHAR(50) NOT NULL CHECK (amenity IN ('4D', 'FOOD_COURT', 'ONLINE_BOOKING', 'IMAX', 'SWEETBOX', 'PARKING', 'DOLBY_ATMOS')),
    PRIMARY KEY (cinema_id, amenity)
);

CREATE TABLE IF NOT EXISTS rooms (
    id UUID PRIMARY KEY,
    cinema_id UUID NOT NULL REFERENCES cinemas(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    format VARCHAR(50) CHECK (format IN ('3D', 'SCREENX', '2D', '4DX', 'IMAX')),
    total_seats INTEGER DEFAULT 0,
    row_count INTEGER NOT NULL,
    column_count INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'MAINTENANCE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_rooms_cinema_name UNIQUE (cinema_id, name)
);

CREATE TABLE IF NOT EXISTS seat_types (
    name VARCHAR(50) PRIMARY KEY CHECK (name IN ('NORMAL', 'VIP', 'SWEETBOX')),
    surcharge NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    description TEXT
);

CREATE TABLE IF NOT EXISTS seats (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    row_char VARCHAR(5) NOT NULL,
    seat_number INTEGER NOT NULL,
    seat_type_name VARCHAR(50) NOT NULL REFERENCES seat_types(name),
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_seats_room_row_number UNIQUE (room_id, row_char, seat_number)
);

CREATE TABLE IF NOT EXISTS genres (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS movies (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    original_title VARCHAR(255),
    synopsis TEXT,
    director VARCHAR(255),
    language VARCHAR(100),
    subtitle VARCHAR(100),
    supported_modes VARCHAR(255),
    is_featured BOOLEAN DEFAULT FALSE,
    age_rating VARCHAR(10),
    duration_minutes INTEGER,
    release_date DATE,
    end_date DATE,
    showing_status VARCHAR(50) NOT NULL CHECK (showing_status IN ('NOW_SHOWING', 'COMING_SOON', 'ENDED')),
    poster_url VARCHAR(500),
    backdrop_url VARCHAR(500),
    trailer_youtube_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DRAFT', 'HIDDEN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS movie_genres (
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    genre_id INTEGER NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (movie_id, genre_id)
);

CREATE TABLE IF NOT EXISTS movie_casts (
    id UUID PRIMARY KEY,
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    actor_name VARCHAR(255) NOT NULL,
    character_name VARCHAR(255),
    role_type VARCHAR(50) CHECK (role_type IN ('DIRECTOR', 'LEAD', 'SUPPORTING', 'CAMEO')),
    avatar_url VARCHAR(500),
    display_order INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS showtimes (
    id UUID PRIMARY KEY,
    movie_id UUID NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    show_date TIMESTAMPTZ NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    language VARCHAR(100),
    subtitle_language VARCHAR(100),
    format VARCHAR(50) CHECK (format IN ('3D', 'SCREENX', '2D', '4DX', 'IMAX')),
    viewing_mode VARCHAR(50) CHECK (viewing_mode IN ('SUBTITLED', 'DUBBED', 'VOICEOVER')),
    base_price NUMERIC(19, 2) NOT NULL,
    available_seats INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS articles (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    slug VARCHAR(500) NOT NULL UNIQUE,
    thumbnail_url VARCHAR(500),
    content TEXT NOT NULL,
    excerpt TEXT,
    category VARCHAR(50) NOT NULL CHECK (category IN ('TIN_DIEN_ANH', 'REVIEW', 'SU_KIEN', 'KHUYEN_MAI', 'CONG_DONG')),
    tags VARCHAR(255),
    author_name VARCHAR(255),
    views INTEGER DEFAULT 0,
    is_featured BOOLEAN DEFAULT FALSE,
    is_trending BOOLEAN DEFAULT FALSE,
    movie_id UUID REFERENCES movies(id) ON DELETE SET NULL,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    event_date DATE,
    event_time TIME,
    location_name VARCHAR(255),
    cinema_id UUID REFERENCES cinemas(id) ON DELETE SET NULL,
    registration_url VARCHAR(500),
    max_attendees INTEGER,
    current_attendees INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'UPCOMING' CHECK (status IN ('UPCOMING', 'ONGOING', 'ENDED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
