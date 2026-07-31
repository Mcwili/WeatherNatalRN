package com.tempo.rn.core.model

data class AppLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

object Locations {
    val PIRANGI_DO_NORTE = AppLocation("pirangi_do_norte", "Pirangi do Norte", -5.981, -35.109)

    val all: List<AppLocation> = listOf(
        PIRANGI_DO_NORTE,
        AppLocation("natal", "Natal", -5.7945, -35.2110),
        AppLocation("pirangi_do_sul", "Pirangi do Sul", -6.035, -35.105),
        AppLocation("ponta_negra", "Ponta Negra", -5.879, -35.181),
        AppLocation("via_costeira", "Via Costeira", -5.845, -35.185),
        AppLocation("cotovelo", "Cotovelo", -5.955, -35.123),
        AppLocation("parnamirim", "Parnamirim", -5.916, -35.263),
        AppLocation("buzios", "Búzios", -6.003, -35.108),
        AppLocation("tabatinga", "Tabatinga", -6.055, -35.100),
        AppLocation("nisia_floresta", "Nísia Floresta", -6.091, -35.208),
        AppLocation("pipa", "Pipa", -6.228, -35.046),
        AppLocation("sao_miguel_do_gostoso", "São Miguel do Gostoso", -5.123, -35.637),
    )

    val default: AppLocation = PIRANGI_DO_NORTE

    fun byId(id: String?): AppLocation = all.firstOrNull { it.id == id } ?: default
}
