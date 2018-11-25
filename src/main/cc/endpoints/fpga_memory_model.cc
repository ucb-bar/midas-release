// See LICENSE for license details.

#include <iostream>
#include <algorithm>
#include <exception>
#include <stdio.h>

#include "fpga_memory_model.h"

FpgaMemoryModel::FpgaMemoryModel(
    simif_t* sim, AddressMap addr_map, int argc, char** argv)
  : FpgaModel(sim, addr_map) {
  std::vector<std::string> args(argv + 1, argv + argc);
  for (auto &arg: args) {
    if (arg.find("+mm-args=") == 0) {
      std::ifstream file(arg.c_str() + 9);
      if (!file) {
        fprintf(stderr, "Cannot open %s\n", arg.c_str() + 9);
        exit(EXIT_FAILURE);
      }
      std::string line;
      while (std::getline(file, line)) {
        if (line.c_str()[0] == '#') {
          // Pass comment
        } else {
          parse_arg(line.c_str());
        }
      }
    }
    if (arg.find("+mm_") == 0) {
      parse_arg(arg.c_str() + 4);
    }
  }
}

void FpgaMemoryModel::parse_arg(const char* arg) {
  auto sub_arg = std::string(arg);
  size_t delimit_idx = sub_arg.find_first_of("=");
  std::string key = sub_arg.substr(0, delimit_idx).c_str();
  int value = std::stoi(sub_arg.substr(delimit_idx+1).c_str());
  model_configuration[key] = value;
}

void FpgaMemoryModel::profile() {
}

void FpgaMemoryModel::init() {
  for (auto &pair: addr_map.w_registers) {
    auto value_it = model_configuration.find(pair.first);
    if (value_it != model_configuration.end()) {
      write(pair.second, value_it->second);
    } else {
      char buf[100];
      sprintf(buf, "No value provided for configuration register: %s", pair.first.c_str());
      throw std::runtime_error(buf);
    }
  }
}

void FpgaMemoryModel::finish() {
}
