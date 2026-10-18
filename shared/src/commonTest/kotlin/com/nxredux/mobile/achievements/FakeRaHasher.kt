package com.nxredux.mobile.achievements

class FakeRaHasher : RaHasher {
    val hashes = HashMap<String, String?>()   // fileName -> md5 (null = unidentified)
    val calls = ArrayList<String>()
    val tags = ArrayList<String>()   // the tag of each call, parallel to calls
    val stamps = HashMap<String, String>()   // ref -> stamp (absent = unknown)
    override suspend fun stamp(ref: String): String? = stamps[ref]
    override suspend fun hash(tag: String, ref: String, fileName: String, folder: String): String? { calls += fileName; tags += tag; return hashes[fileName] }
}
