/*
 *     Copyright (C) 2025 nift4
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#ifndef GRAMOPHONE_HELPERS_H
#define GRAMOPHONE_HELPERS_H

#define DLSYM_OR_ELSE(LIB, FUNC) if (!FUNC) { \
    if (!LIB##_handle) { \
        ALOGE("dlsym handle of " #LIB ".so is not open - code bug"); \
    } else { \
        FUNC = (FUNC##_t) dlsym(LIB##_handle, "_" #FUNC); \
        if (!FUNC) { \
            ALOGI("dlsym returned nullptr for _" #FUNC " in " #LIB ".so: %s", dlerror()); \
        } \
    } \
} \
if (!FUNC)

#define DLSYM_OR_RETURN(LIB, FUNC, RET) if (!FUNC) { \
    if (!LIB##_handle) { \
        ALOGE("dlsym handle of " #LIB ".so is not open - code bug"); \
        return RET; \
    } \
    FUNC = (FUNC##_t) dlsym(LIB##_handle, "_" #FUNC); \
    if (!FUNC) { \
        ALOGE("dlsym returned nullptr for _" #FUNC " in " #LIB ".so: %s", dlerror()); \
        return RET; \
    } \
}

#endif //GRAMOPHONE_HELPERS_H
