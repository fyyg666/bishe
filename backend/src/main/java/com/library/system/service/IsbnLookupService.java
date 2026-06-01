package com.library.system.service;

import com.library.system.dto.IsbnLookupResponse;
import java.util.Optional;

public interface IsbnLookupService {

    Optional<IsbnLookupResponse> lookup(String isbn);
}
