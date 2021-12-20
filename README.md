# Speed comparison between native and Java OpenCV calls
This is just a quick and dirty branch for measuring OpenCV timings.

Mat size is 3000x4000.

## Pixel 4A (in seconds):
| Operation                     | C++   | Java  |
| -------------                 |:-----:|:-----:|
| Accessing mat pixel-wise     | 0.3   | 6     |
| cv::remap (INTER_LANCZOS4)    | 0.75  | 0.75  |
| cv::remap (INTER_LINEAR)      | 0.08  | 0.08  |

## Samsung S6 (in seconds):
| Operation                     | C++   | Java  |
| -------------                 |:-----:|:-----:|
| Accessing mat pixel-wise      | 0.85  | 45     |
| cv::remap (INTER_LANCZOS4)    | 1.3   | 1.3    |
| cv::remap (INTER_LINEAR)      | 0.1  | 0.1  |
| cv::remap (INTER_CUBIC)       | 0.14  | 0.14  |
