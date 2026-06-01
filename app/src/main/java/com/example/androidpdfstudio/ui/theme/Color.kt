package com.example.androidpdfstudio.ui.theme

import androidx.compose.ui.graphics.Color

// ── Fixed Studio Colors (match iOS hex values exactly) ──────────────────────
val NavyDark   = Color(0xFF0F172A)
val NavyMedium = Color(0xFF1E293B)
val StudioAccent = Color(0xFF3B82F6)
val StudioRed    = Color(0xFFEF4444)
val StudioGreen  = Color(0xFF10B981)
val StudioTeal   = Color(0xFF14B8A6)

// ── Section Gradient Palettes (matching iOS SectionPalette enum) ─────────────
object SectionPalette {
    // Home Dashboard — warm vibrant: Orange → Pink → Purple
    val home   = listOf(Color(0xFFFFB000), Color(0xFFFF5F6D), Color(0xFFA044FF))
    // Edit PDF — cool productivity: Blue → Cyan → Indigo
    val edit   = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4), Color(0xFF6366F1))
    // Merge PDF — modern growth: Green → Teal
    val merge  = listOf(Color(0xFF10B981), Color(0xFF14B8A6))
    // Split PDF — creative: Purple → Violet → Pink
    val split  = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFEC4899))
    // Photos to PDF — energetic: Yellow → Orange → Red
    val photos = listOf(Color(0xFFFACC15), Color(0xFFF97316), Color(0xFFEF4444))
    // Compress PDF — warm: Deep Orange → Amber
    val compress = listOf(Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFFBBF24))
    // PDF to Images — cool: Sky → Indigo
    val toImages = listOf(Color(0xFF0284C7), Color(0xFF4F46E5), Color(0xFF7C3AED))
}
