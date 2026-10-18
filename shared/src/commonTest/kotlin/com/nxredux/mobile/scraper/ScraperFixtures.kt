package com.nxredux.mobile.scraper

object ScraperFixtures {
    val GAME_FOUND = """
    {"header":{"success":"true"},"response":{"jeu":{"id":"1","noms":[{"region":"wor","text":"Sonic"}],"medias":[
      {"type":"sstitle","region":"us","url":"https://m/sstitle-us.png","format":"png"},
      {"type":"ss","region":"jp","url":"https://m/ss-jp.png","format":"png"},
      {"type":"ss","region":"eu","url":"https://m/ss-eu.png","format":"png"},
      {"type":"box-2D","region":"us","url":"https://m/box2d-us.png","format":"png"},
      {"type":"box-3D","region":"wor","url":"https://m/box3d-wor.png","format":"png"},
      {"type":"wheel","region":"us","url":"https://m/wheel-us.png","format":"png"},
      {"type":"fanart","url":"https://m/fanart.jpg","format":"jpg"}
    ]}}}""".trimIndent()
    val GAME_NO_JEU = """{"header":{"success":"true"},"response":{}}"""
    val GARBAGE = "Erreur : Rom/Iso/Dossier non trouvée ! "

    // Real ScreenScraper bodies, verified live (text only; no credentials).
    /** jeuInfos 404 for an unknown ROM. */
    const val ROM_NOT_FOUND = "Erreur : Rom/Iso/Dossier non trouvée !"
    /** jeuInfos / ssuserInfos 403 with a bad developer password. */
    const val DEV_LOGIN_REFUSED = "Erreur de login : Vérifier vos identifiants développeur !"
    /** ssuserInfos 403 with a wrong user login (jeuInfos answers 200 instead, as anonymous). */
    const val USER_LOGIN_REFUSED = "Erreur de login : Vérifier les identifiants utilisateurs !"
    val TRUNCATED = """{"response":{"jeu":{"medias":[{"type":"ss","""
    val QUOTA_STRINGS = """{"response":{"ssuser":{"id":"me","requeststoday":"1234","maxrequestsperday":"20000"}}}"""
    val QUOTA_NUMBERS = """{"response":{"ssuser":{"requeststoday":5,"maxrequestsperday":10000}}}"""
}
