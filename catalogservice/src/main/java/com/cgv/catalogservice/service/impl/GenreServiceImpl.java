package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.genre.GenreCreateRequest;
import com.cgv.catalogservice.dto.request.genre.GenreUpdateRequest;
import com.cgv.catalogservice.dto.response.GenreResponse;
import com.cgv.catalogservice.entity.Genre;
import com.cgv.catalogservice.exception.ResourceConflictException;
import com.cgv.catalogservice.mapper.GenreMapper;
import com.cgv.catalogservice.repository.GenreRepository;
import com.cgv.catalogservice.repository.MovieGenreRepository;
import com.cgv.catalogservice.service.GenreService;
import com.cgv.catalogservice.util.PageResponseUtils;
import com.cgv.catalogservice.util.SlugUtils;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j(topic = "GENRE-SERVICE")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenreServiceImpl implements GenreService {

    GenreRepository genreRepository;
    MovieGenreRepository movieGenreRepository;

    GenreMapper genreMapper;

    @Override
    @Transactional
    @CacheEvict(value = "genres:all", allEntries = true)
    public GenreResponse createGenre(
            GenreCreateRequest request
    ) {

        log.info("Creating genre: name={}", request.name());
        String name = request.name();

        if (genreRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException(
                    "Thể loại với tên '" + name + "' đã tồn tại"
            );
        }

        String slug = SlugUtils.toSlug(name);

        if (genreRepository.existsBySlug(slug)) {
            throw new ResourceConflictException(
                    "Slug '" + slug + "' đã tồn tại"
            );
        }

        Genre genre = genreMapper.toEntity(request);

        genre.setSlug(slug);

        Genre savedGenre = genreRepository.save(genre);

        return genreMapper.toResponse(savedGenre);
    }

    @Override
    @Transactional
    @CacheEvict(value = "genres:all", allEntries = true)
    public GenreResponse updateGenre(
            Integer genreId,
            GenreUpdateRequest request
    ) {

        log.info("Updating genre: genreId={}", genreId);
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Không tìm thấy genre với id: " + genreId
                        )
                );

        String name = request.name();

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Tên thể loại không được để trống"
            );
        }

        if (genreRepository.existsByNameIgnoreCaseAndIdNot(
                name,
                genreId
        )) {
            throw new ResourceConflictException(
                    "Thể loại với tên '" + name + "' đã tồn tại"
            );
        }

        String slug = SlugUtils.toSlug(name);

        if (genreRepository.existsBySlugAndIdNot(
                slug,
                genreId
        )) {
            throw new ResourceConflictException(
                    "Slug '" + slug + "' đã tồn tại"
            );
        }

        genreMapper.updateEntity(request, genre);

        genre.setSlug(slug);

        return genreMapper.toResponse(genre);
    }

    @Override
    @Transactional
    @CacheEvict(value = "genres:all", allEntries = true)
    public void deleteGenre(Integer genreId) {

        log.info("Deleting genre: genreId={}", genreId);

        if (movieGenreRepository.existsByIdGenreId(genreId)) {
            throw new ResourceConflictException(
                    "Không thể xoá genre vì đang có phim sử dụng thể loại này"
            );
        }

        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy genre với id: " + genreId
                ));

        genreRepository.delete(genre);
    }

    @Override
    @Transactional(readOnly = true)
    public GenreResponse getGenreById(Integer genreId) {

        log.debug("Getting genre by id: genreId={}", genreId);
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy genre với id: " + genreId
                ));

        return genreMapper.toResponse(genre);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "genres:all", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public PageResponse<GenreResponse> getAllGenres(Pageable pageable) {

        log.debug("Getting all genres: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        return PageResponseUtils.findAllAndMap(
                genreRepository::findAll,
                pageable,
                genreMapper::toResponseList
        );
    }
}
