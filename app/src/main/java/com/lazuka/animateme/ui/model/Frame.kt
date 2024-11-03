package com.lazuka.animateme.ui.model

data class Frame(
    val previousDrawnPaths: List<DrawnPath>,
    val drawnPaths: List<DrawnPath> = emptyList()
)
