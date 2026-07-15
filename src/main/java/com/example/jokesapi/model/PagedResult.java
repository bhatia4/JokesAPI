package com.example.jokesapi.model;

import java.util.List;

public record PagedResult<T>(List<T> items, String nextCursor) {}
