package com.nxredux.mobile.achievements

import com.nxredux.mobile.library.FakeRomsSource
import com.nxredux.mobile.runner.ContentRef

fun fakePlatform(
    store: FakeRaStore = FakeRaStore(), http: FakeRaHttp = FakeRaHttp(), hasher: FakeRaHasher = FakeRaHasher(), connectivity: FakeConnectivity = FakeConnectivity(),
    home: RaHomeTree = RaHomeTree(FakeRomsSource(mapOf("home" to listOf("Roms"), "home/Roms" to emptyList()))) { ContentRef("home") },
) = RaPlatform(store, http, hasher, FakeRaSigner(), connectivity, home)
