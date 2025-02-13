version = 1


cloudstream {
    language = "es"
    // All of these properties are optional, you can safely remove them

    description = "One Pace ES"
    authors = listOf("KillerDogeEmpire,Phisher98,sinister-kid")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime"
    )
    iconUrl = "https://raw.githubusercontent.com/sinister-kid/sinicloud/master/icons/onepacees-icon.png"

    isCrossPlatform = true
}
