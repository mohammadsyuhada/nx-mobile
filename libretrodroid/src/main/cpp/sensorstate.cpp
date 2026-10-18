/*
 *     Copyright (C) 2026  Mohammad Syuhada
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include "sensorstate.h"

#include "libretro.h"

namespace libretrodroid {

SensorState& SensorState::get() { static SensorState instance; return instance; }

void SensorState::setAvailable(bool accel, bool gyro) { std::lock_guard<std::mutex> g(lock); hasAccel = accel; hasGyro = gyro; }

void SensorState::setEnabled(unsigned port, bool on) { if (port != 0) return; std::lock_guard<std::mutex> g(lock); enabled = on; }

bool SensorState::setState(unsigned port, unsigned action) {
    std::lock_guard<std::mutex> g(lock);
    switch (action) {
        case RETRO_SENSOR_ACCELEROMETER_ENABLE: return port == 0 && enabled && hasAccel;
        case RETRO_SENSOR_GYROSCOPE_ENABLE: return port == 0 && enabled && hasGyro;
        default: return true;   // the *_DISABLE actions (and illuminance, unused)
    }
}

void SensorState::setValues(unsigned port, float ax, float ay, float az, float gx, float gy, float gz) {
    if (port != 0) return;
    std::lock_guard<std::mutex> g(lock);
    values = {ax, ay, az, gx, gy, gz};
}

float SensorState::input(unsigned port, unsigned id) const {
    std::lock_guard<std::mutex> g(lock);
    if (port != 0 || !enabled) return 0.f;
    switch (id) {
        case RETRO_SENSOR_ACCELEROMETER_X: return values[0];
        case RETRO_SENSOR_ACCELEROMETER_Y: return values[1];
        case RETRO_SENSOR_ACCELEROMETER_Z: return values[2];
        case RETRO_SENSOR_GYROSCOPE_X: return values[3];
        case RETRO_SENSOR_GYROSCOPE_Y: return values[4];
        case RETRO_SENSOR_GYROSCOPE_Z: return values[5];
        default: return 0.f;
    }
}

void SensorState::reset() { std::lock_guard<std::mutex> g(lock); hasAccel = hasGyro = enabled = false; values = {}; }

}
