package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.dto.request.genre.GenreCreateRequest;
import com.cgv.catalogservice.dto.request.genre.GenreUpdateRequest;
import com.cgv.catalogservice.dto.response.GenreResponse;
import com.cgv.catalogservice.entity.Genre;
import com.cgv.catalogservice.mapper.GenreMapper;
import com.cgv.catalogservice.repository.GenreRepository;
import com.cgv.catalogservice.service.GenreService;
import com.cgv.commondto.dto.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenreServiceImpl implements GenreService {

    GenreRepository genreRepository;
    GenreMapper genreMapper;

    @Override
    @Transactional
    public GenreResponse createGenre(GenreCreateRequest request) {
        Genre genre = genreMapper.toEntity(request);
        Genre savedGenre = genreRepository.save(genre);

        return genreMapper.toResponse(savedGenre);
    }

    @Override
    @Transactional
    public GenreResponse updateGenre(Integer genreId, GenreUpdateRequest request) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy genre với id: " + genreId
                ));

        genreMapper.updateEntity(request, genre);

        return genreMapper.toResponse(genre);
    }

    @Override
    @Transactional
    public void deleteGenre(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy genre với id: " + genreId
                ));

        genreRepository.delete(genre);
    }

    @Override
    @Transactional(readOnly = true)
    public GenreResponse getGenreById(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy genre với id: " + genreId
                ));

        return genreMapper.toResponse(genre);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GenreResponse> getAllGenres(Pageable pageable) {
        Page<Genre> genrePage = genreRepository.findAll(pageable);
        List<GenreResponse> genreResponses = genreMapper.toResponseList(genrePage.getContent());

        return PageResponse.<GenreResponse>builder()
                .data(genreResponses)
                .pageNumber(genrePage.getNumber() + 1)
                .pageSize(genrePage.getSize())
                .totalPages(genrePage.getTotalPages())
                .totalElements(genrePage.getTotalElements())
                .build();
    }
}
