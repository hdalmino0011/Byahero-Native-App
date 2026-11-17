cmake_minimum_required(VERSION 3.22.1)
project("byahero_native")

add_library(
    byahero_native
    SHARED
    byahero_native.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    byahero_native
    ${log-lib}
)
