package com.rag.knowledgehub.service;

import java.util.List;

public interface EmbeddingService {

    List<Double> embed(String text);
}
