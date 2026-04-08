from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI(title="Sentence-BERT Embedding Service")
model = SentenceTransformer("all-MiniLM-L6-v2")


class EmbedRequest(BaseModel):
    model: str | None = None
    text: str


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/embed")
def embed(req: EmbedRequest):
    vector = model.encode(req.text, normalize_embeddings=True)
    return {"embedding": vector.tolist(), "dim": len(vector)}
