package com.cgv.catalogservice.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Document(indexName = "movies")
@Setting(settingPath = "/elasticsearch/movie-analyzer.json")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDocument {

    @Id
    private String id;

    // Tìm kiếm đa năng: vừa search tiếng Việt không dấu, vừa gõ gợi ý autocomplete
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "vietnamese_search_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "vietnamese_search_analyzer")
    private String originalTitle;

    @Field(type = FieldType.Text, analyzer = "vietnamese_search_analyzer")
    private String synopsis;

    @Field(type = FieldType.Text, analyzer = "vietnamese_search_analyzer")
    private String director;

    @Field(type = FieldType.Keyword)
    private String ageRating;

    @Field(type = FieldType.Keyword)
    private String showingStatus; // NOW_SHOWING, COMING_SOON

    @Field(type = FieldType.Keyword)
    private List<String> genres;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate releaseDate;
}