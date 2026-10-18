package com.nxredux.mobile.runner

import kotlin.jvm.JvmInline

/** Opaque reference to a piece of content on the platform: on Android a document URI string. */
@JvmInline
value class ContentRef(val value: String)
