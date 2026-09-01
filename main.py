from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="三接口演示平台", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

from middleware.tracking import TrackingMiddleware

app.add_middleware(TrackingMiddleware)

from models.tracking import init_db

init_db()

from apis.helloworld import router as helloworld_router
from apis.hash_api import router as hash_router
from apis.bubble_sort import router as bubble_sort_router
from apis.analytics import router as analytics_router
from export.csv_writer import router as export_router

app.include_router(helloworld_router)
app.include_router(hash_router)
app.include_router(bubble_sort_router)
app.include_router(analytics_router)
app.include_router(export_router)


@app.get("/health")
def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)