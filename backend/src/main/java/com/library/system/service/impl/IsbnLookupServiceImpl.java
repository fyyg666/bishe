package com.library.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.IsbnLookupResponse;
import com.library.system.service.IsbnLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IsbnLookupServiceImpl implements IsbnLookupService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Override
    public Optional<IsbnLookupResponse> lookup(String isbn) {
        isbn = isbn.replace("-", "").replace(" ", "");
        if (isbn.length() < 10) {
            return Optional.empty();
        }

        Optional<IsbnLookupResponse> result = lookupOpenLibrary(isbn);
        if (result.isPresent()) {
            return result;
        }

        return lookupGoogleBooks(isbn);
    }

    private Optional<IsbnLookupResponse> lookupOpenLibrary(String isbn) {
        try {
            String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn
                    + "&format=json&jscmd=data";
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return Optional.empty();

            JsonNode root = objectMapper.readTree(response);
            JsonNode bookData = root.get("ISBN:" + isbn);
            if (bookData == null) return Optional.empty();

            IsbnLookupResponse.IsbnLookupResponseBuilder builder = IsbnLookupResponse.builder()
                    .isbn(isbn)
                    .source("Open Library");

            if (bookData.has("title")) builder.title(bookData.get("title").asText());

            if (bookData.has("authors") && bookData.get("authors").isArray() && !bookData.get("authors").isEmpty()) {
                JsonNode firstAuthor = bookData.get("authors").get(0);
                if (firstAuthor.has("name")) builder.author(firstAuthor.get("name").asText());
            }

            if (bookData.has("publishers") && bookData.get("publishers").isArray() && !bookData.get("publishers").isEmpty()) {
                JsonNode firstPublisher = bookData.get("publishers").get(0);
                if (firstPublisher.has("name")) builder.publisher(firstPublisher.get("name").asText());
            }

            if (bookData.has("publish_date")) builder.publishDate(bookData.get("publish_date").asText());

            if (bookData.has("cover")) {
                JsonNode cover = bookData.get("cover");
                if (cover.has("large")) builder.coverUrl(cover.get("large").asText());
                else if (cover.has("medium")) builder.coverUrl(cover.get("medium").asText());
                else if (cover.has("small")) builder.coverUrl(cover.get("small").asText());
            }

            if (bookData.has("number_of_pages")) builder.pageCount(bookData.get("number_of_pages").asInt());

            return Optional.of(builder.build());
        } catch (Exception e) {
            log.warn("Open Library查询失败: isbn={}, error={}", isbn, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<IsbnLookupResponse> lookupGoogleBooks(String isbn) {
        try {
            String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return Optional.empty();

            JsonNode root = objectMapper.readTree(response);
            if (root.has("totalItems") && root.get("totalItems").asInt() == 0) return Optional.empty();

            JsonNode items = root.get("items");
            if (items == null || !items.isArray() || items.isEmpty()) return Optional.empty();

            JsonNode volumeInfo = items.get(0).get("volumeInfo");
            if (volumeInfo == null) return Optional.empty();

            IsbnLookupResponse.IsbnLookupResponseBuilder builder = IsbnLookupResponse.builder()
                    .isbn(isbn)
                    .source("Google Books");

            if (volumeInfo.has("title")) builder.title(volumeInfo.get("title").asText());

            if (volumeInfo.has("authors") && volumeInfo.get("authors").isArray()) {
                StringBuilder authors = new StringBuilder();
                for (JsonNode author : volumeInfo.get("authors")) {
                    if (authors.length() > 0) authors.append(", ");
                    authors.append(author.asText());
                }
                builder.author(authors.toString());
            }

            if (volumeInfo.has("publisher")) builder.publisher(volumeInfo.get("publisher").asText());
            if (volumeInfo.has("publishedDate")) builder.publishDate(volumeInfo.get("publishedDate").asText());
            if (volumeInfo.has("description")) builder.description(volumeInfo.get("description").asText());
            if (volumeInfo.has("pageCount")) builder.pageCount(volumeInfo.get("pageCount").asInt());

            if (volumeInfo.has("imageLinks")) {
                JsonNode images = volumeInfo.get("imageLinks");
                if (images.has("large")) builder.coverUrl(images.get("large").asText());
                else if (images.has("medium")) builder.coverUrl(images.get("medium").asText());
                else if (images.has("thumbnail")) builder.coverUrl(images.get("thumbnail").asText());
            }

            return Optional.of(builder.build());
        } catch (Exception e) {
            log.warn("Google Books查询失败: isbn={}, error={}", isbn, e.getMessage());
            return Optional.empty();
        }
    }
}
