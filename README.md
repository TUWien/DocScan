# Speed comparison between native and Java OpenCV calls
This is just a quick and dirty branch for measuring OpenCV timings.

Results gained on Pixel 4A (in seconds). Mat size is 3000x4000.

| Operation                     | C++   | Java  |
| -------------                 |:-----:|:-----:|
| Accessing mat pixel-wise     | 0.3   | 6     |
| cv::remap (INTER_LANCZOS4)    | 0.75  | 0.75  |
| cv::remap (INTER_LINEAR)      | 0.08  | 0.08  |
| cv::remap (INTER_CUBIC)       | 0.05  | 0.05  |
