package com.cgv.catalogservice.repository.search;

import com.cgv.catalogservice.document.MovieDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieSearchRepository extends ElasticsearchRepository<MovieDocument, String> {

    // Tìm kiếm mờ (Fuzzy: sai 1-2 ký tự vẫn ra) và tìm kiếm không dấu
    @Query("""
    {
      "multi_match": {
        "query": "?0",
        "fields": ["title^3", "originalTitle^2", "synopsis", "director"],
        "fuzziness": "AUTO"
      }
    }
    """)
    Page<MovieDocument> searchFuzzy(String keyword, Pageable pageable);
}