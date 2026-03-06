#include <stdio.h>
#include <stdlib.h>
#include <mpv/client.h>
#include <mpv/render.h>

int main() {
    printf("MPV_RENDER_PARAM_API_TYPE: %d\n", MPV_RENDER_PARAM_API_TYPE);
    printf("MPV_RENDER_PARAM_SW_SIZE: %d\n", MPV_RENDER_PARAM_SW_SIZE);
    printf("MPV_RENDER_PARAM_SW_FORMAT: %d\n", MPV_RENDER_PARAM_SW_FORMAT);
    printf("MPV_RENDER_PARAM_SW_STRIDE: %d\n", MPV_RENDER_PARAM_SW_STRIDE);
    printf("MPV_RENDER_PARAM_SW_POINTER: %d\n", MPV_RENDER_PARAM_SW_POINTER);
    return 0;
}
