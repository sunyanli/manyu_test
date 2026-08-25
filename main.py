from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from routers import helloworld, hash, bubble_sort, export

app = FastAPI(title="Tool API", version="1.0.0")

# CORS — 开发阶段全放通
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 挂载路由
app.include_router(helloworld.router, prefix="/api", tags=["helloworld"])
app.include_router(hash.router, prefix="/api", tags=["hash"])
app.include_router(bubble_sort.router, prefix="/api", tags=["bubble_sort"])
app.include_router(export.router, prefix="/api", tags=["export"])


@app.get("/")
def root():
    return {"status": "ok", "service": "Tool API"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)