package com.kiyamet.help;

import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** /twd help - Türkçe oyun rehberi: her konu tıklanabilir. */
public final class TwdHelp {
    record Topic(String title, ChatFormatting color, String[] lines) {}
    static final Map<String, Topic> TOPICS = new LinkedHashMap<>();

    static void t(String id, String title, ChatFormatting c, String... lines) { TOPICS.put(id, new Topic(title, c, lines)); }

    static {
        t("baslangic", "İlk adımlar", ChatFormatting.GREEN,
                "Oyun 1 Ocak 2027'de, kışın başlar. Yanında bir yoldaş doğar: odun, taş, demir toplar.",
                "1) Su barın açlık barının üstünde. Matara yap, suyla doldur, nehir suyunu kamp ateşinde kaynat.",
                "2) Kışın üşürsün: zırhın üstüne kürk giy (hayvan postlarından). Ateş yanında ısın.",
                "3) Ana Masa yap (3 demir + 5 tahta + çalışma masası). Bütün TWD masaları orada yapılır.",
                "4) Gece walkerlar artar. Üs kur, taret ve elektrikli çit koy.",
                "5) K tuşu: yeteneklerin (20 yetenek, 999 seviyeye kadar). Her şey yetenek XP'si verir.");
        t("masalar", "Masalar", ChatFormatting.GOLD,
                "Ana Masa: Mobilya, Pişirme, Silah Masası, Laboratuvar, Araç Atölyesi, Jeneratör, Güneş Paneli,",
                "  Rüzgar Türbini, Matkap, Testere, Yerleşim Çanı ve Görev Panosu sadece burada yapılır.",
                "Pişirme Masası: 30 yemeğin hepsi burada pişer. Shift + tık = yapabildiğin kadar pişir.",
                "Silah Masası: BUILD GUNS ile bütün silah ve mermiler; sekmelerle dürbün, namlu, şarjör, boya, tamir.",
                "Mobilya Masası: yataklar, dolaplar, buzdolabı, koltuk... 20 mobilya.",
                "Araç Atölyesi: 19 araç. Örs: element çekirdeği + Kadim Çubuk = element silahı.");
        t("hayatta", "Hayatta kalma", ChatFormatting.AQUA,
                "Su: yazın ve koşarken hızlı düşer. 6 altı yavaşlatır, 0'da can kaybedersin. 5 matara: 4-20 yudum.",
                "Sıcaklık: kış ve kar fırtınası üşütür. Kürk + ateş + husky yanında olması ısıtır.",
                "Yemek bozulur: üstünde son kullanma tarihi var. Buzdolabı bozulmayı çok yavaşlatır.",
                "Tarih ve mevsim: /twd date. Her gün sohbette mevsim olayı yazar (kar fırtınası, sıcak hava...).",
                "Yemek: Pişirme Masasında 30 tarif. İyi Doymuş etkisi verir.",
                "Tabak (2 kil + kemik tozu = 2 tabak): masaya koy, yemekle sağ tık = yemek tabağa. Boş el: geri al, eğil + boş el: tabaktan ye.");
        t("silahlar", "Silahlar", ChatFormatting.RED,
                "R: şarjörü doldur / tutukluğu aç. Boş şarjörle tetiğe basarsan kendi kendine doldurur.",
                "Her atış silahı aşındırır, aşındıkça tutukluk yapar. Silah Masasında tamir et.",
                "Mühendislik yeteneği yükseldikçe daha az tutukluk ve aşınma olur.",
                "Silahına isim: /twd gunname <isim>. Örs de olur.",
                "Mermi: Silah Masası → BUILD GUNS, son sayfa.");
        t("hayvanlar", "Hayvanlar", ChatFormatting.YELLOW,
                "Aslan (savan), kaplan (orman), ayı (orman/tayga), buz ayısı (kar; kışın dağlardan iner), timsah (bataklık).",
                "Yırtıcılar hemen saldırmaz: seni izler. Çok yakında beklersen koşar gelir, çok sert vurur. Eğilmek yardımcı olur.",
                "Aslan/kaplan evcilleştirme: 20 çiğ geyik eti ver. %65 senin olur, yoksa saldırır (Avcılık şansı artırır).",
                "Evcil: Alman çoban (walker koklar), husky (ısıtır), rottweiler, siyah pitbull (kemik/et); kedi (balık).",
                "Walker hayvanlar nadirdir, görünce saldırır. Ölen hayvanlar ve kurtlar walker olarak dirilir - yakarak öldür!");
        t("yoldas", "Yoldaşlar, yerleşim, aile", ChatFormatting.LIGHT_PURPLE,
                "Yoldaşa sağ tık: panel. Takip et, bekle, nöbet, kaynak topla, araba sür, yerleş.",
                "Moral düşerse yoldaş gidebilir; çok nadiren ihanet edip bir eşyanı çalabilir. Onları doyur, koru.",
                "Yerleşim Çanı koy → yoldaş panelinde 'Settle here'. Çana tıkla: meslek seç (çiftçi, demirci, doktor, muhafız, aşçı).",
                "Çanın 12 blok yakınındaki sandıklar depodur. Herkes her sabah 1 yemek yer ve yatak ister.",
                "Aile: morali 60+ kadın yoldaşa paneldeki çiçekten teklif et. Geceleri yakınında uyu. 9 ay sonra çocuk. /twd family",
                "Bebek ilk ay beyaz bir örtüye sarılı, çantanın yanında seninle gelir (çanta yoksa sırtında). Büyüyünce kendi yürür.");
        t("elektrik", "Elektrik", ChatFormatting.BLUE,
                "Jeneratör (yakıtlı), Güneş Paneli (gündüz, gökyüzünü görmeli), Rüzgar Türbini (gece gündüz).",
                "8 blok içindeki taretler, elektrikli çitler, ışıklar çalışır.",
                "Matkap (3x3 kazar) ve Testere (bütün ağacı keser) elektrikli alanda dururken şarj olur. Eğilirsen tek blok.");
        t("element", "Elementler ve tapınaklar (2.0)", ChatFormatting.DARK_PURPLE,
                "1) Şehir sandıklarından Yırtık Şehir Haritası bul. Sağ tıkla: bir bilgenin yerini gösterir.",
                "2) Bilgeye git, konuş. Sana Kadim Çubuk emanet eder. Onunla 20 walker öldür ve istediklerini getir.",
                "3) Bilge sana tapınağın haritasını verir (koordinatlar haritanın açıklamasında).",
                "4) Tapınakta 3 kat aşağı in: koruyucular, labirent, en altta Lord. En dipteki sandıkta element çekirdeği.",
                "5) Çekirdek elde, çubuk envanterde, örse sağ tıkla: element silahın doğar.",
                "Silahta sol tık saldırı, sağ tık güç aç/kapat (bekleme yok). 14 element, 14 güç.",
                "Bilgeler ölümsüz. Toplam yetenek gücün 3000 olunca yardım ettiğin bilge yoldaşın olur;",
                "  eğil + sağ tıkla ona element silahı ver, onunla savaşır.");
        t("yaratilis", "Yaratılış (2.2)", ChatFormatting.WHITE,
                "Yaratılış en büyük element. 14 elementin bütün güçleri onda.",
                "1) 12 taşı topla: eski 10 taş + End'den 2 yeni taş (Yıldız Taşı: Rasathanenin Bekçisi, Ejder Taşı: Boşluk Tiranı).",
                "2) End'e git. Ejderin adasının güneyinde Bilgelik Tapınağı var (X 0, Z 150): beyaz, küçük, kırılmaz.",
                "   Beyaz köprüyü takip et. Ortadaki Yaratılış Masası alınamaz, sadece orada çalışır.",
                "3) Masada: 12 taş (eldivenden çıkar, envanterde dursun) = Yaratılış Elementi.",
                "   14 element silahı = Yaratılış Çubuğu.",
                "4) Yaratılış Elementi elde, çubuk envanterde, örse sağ tık: YARATILIŞ BALTASI (çift taraflı savaş baltası).",
                "Balta: her gücün kendi tuşu var (Ayarlar > Kontroller > The Walking Dead, varsayılan numpad 1-9, 0, / * - +).",
                "  Sağ tık: seçili gücü aç/kapat · eğil + sağ tık: sonraki güç. Aynı anda birçok güç açık olabilir;",
                "  zaman güçleri (buz, gök gürültüsü, rüzgar) ve uçuş güçleri (ruh, hava) birbirinin yerine geçer.",
                "5) 20 yeteneğin hepsi 999 olunca Yaratılış'ın Bilgesi olursun. Sana Yaratılış Sunağı verilir.",
                "   Sunağı koy, tapınağını etrafına kendin inşa et. Yoldaşlarını getir, eğil + sağ tıkla sunağa:",
                "   tapınağın koruyucuları olurlar, ölmezler. Yakında yoldaş yokken eğil + sağ tık: hepsini serbest bırakır.");
        t("end", "End yapıları (2.2)", ChatFormatting.LIGHT_PURPLE,
                "Dış End adalarında (merkezden 1150+ blok) 5 büyük yapı tekrar tekrar çıkar, adaların çok üstünde (Y 140-200) süzülür:",
                "Boşluk Kalesi - 4 kuleli kale, kalede Boşluk Tiranı. Ejder Taşı ondan düşer.",
                "Gök Kulesi - 96 blok kule, içinde döner merdiven, 4 gök platformu. Tepede Gök Habercisi.",
                "Düşmüş Gök Gemisi - ortadan kırılmış dev gemi. Kaptan kamarasında Ölü Kaptan.",
                "Ender Kovanı - içi oyuk dev küre, yumurtalar ve raflar. Dipte Kovan Anası.",
                "Yıldız Düşüşü Rasathanesi - dev halka ve kubbe, teleskop. Kubbede Yıldız Bekçisi. Yıldız Taşı ondan düşer.",
                "Zombi Endermanlar: bakmasan da saldırır, arkana ışınlanır, ısırığı enfekte eder. Su onları da yakar.",
                "Sandıklarda nadiren eski taşlar da çıkar.");
        t("komutlar", "Komutlar", ChatFormatting.WHITE,
                "/twd help - bu rehber   /skills - yetenekler   /twd date - tarih ve mevsim",
                "/twd family - ailen   /twd gunname <isim> - silahına isim   /twd drive - otopilot",
                "/twd mind - akıl/moral   /twd raidinfo - baskın bilgisi   /twd season - mevsim",
                "Yaratıcı mod: /twd temple <element>, /twd sage <element>, /twd buildtemple <element>, /twd date set yyyy-aa-gg",
                "Yaratıcı mod (2.2): /twd endfind <yapı>, /twd endbuild <yapı>, /twd buildwisdom, /twd ascend");
        t("tuslar", "Tuşlar", ChatFormatting.GRAY,
                "K: yetenekler   P: süper güçler   H: yoldaşlar takip/bekle   G: grup paneli",
                "R: silah doldur / eldiven taşı gücü   V: sonraki taş   B: walker bağırsağı sür (kamuflaj)",
                "N: gece görüşü   J: gölge adımı   U: giyilen çantayı aç   Y: otopilot (Xaero işaretine sür)",
                "Element silahı: sağ tık güç aç/kapat. Çanta: eğilip yere sağ tık = yere koy.",
                "Yaratılış Baltası: numpad 1-9, 0, /, *, -, + = 14 güç (Kontroller'den değiştir).",
                "E: sırtında çanta varsa çanta ve envanterin birlikte açılır. Eğil + E: normal envanter (zırh, üretim).");
    }

    static MutableComponent link(String id, Topic tp) {
        return Component.literal("[" + tp.title() + "]").withStyle(s -> s.withColor(tp.color()).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/twd help " + id)).withHoverEvent(new HoverEvent.ShowText(Component.literal("Tıkla: " + tp.title()))));
    }

    static int index(ServerPlayer p) {
        p.sendSystemMessage(Component.literal("━━━ The Walking Dead - Yardım ━━━").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.literal("Bir konuya tıkla:").withStyle(ChatFormatting.GRAY));
        MutableComponent line = Component.empty(); int n = 0;
        for (var e : TOPICS.entrySet()) {
            line.append(link(e.getKey(), e.getValue())).append(Component.literal("  "));
            if (++n % 4 == 0) { p.sendSystemMessage(line); line = Component.empty(); }
        }
        if (n % 4 != 0) p.sendSystemMessage(line);
        return 1;
    }

    static int topic(ServerPlayer p, String id) {
        Topic tp = TOPICS.get(id);
        if (tp == null) return index(p);
        p.sendSystemMessage(Component.literal("━━ " + tp.title() + " ━━").withStyle(tp.color(), ChatFormatting.BOLD));
        for (String l : tp.lines()) p.sendSystemMessage(Component.literal(l).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(Component.literal("« tüm konular").withStyle(s -> s.withColor(ChatFormatting.GRAY).withUnderlined(true).withClickEvent(new ClickEvent.RunCommand("/twd help"))));
        return 1;
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> {
            var help = Commands.literal("help").executes(c -> index(c.getSource().getPlayerOrException()));
            for (String id : TOPICS.keySet()) help.then(Commands.literal(id).executes(c -> topic(c.getSource().getPlayerOrException(), id)));
            d.register(Commands.literal("twd").then(help));
        });
        // first join: point at the guide once
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((h, s, server) -> {
            if (h.player.getTags().contains("twd_help_seen")) return;
            h.player.addTag("twd_help_seen");
            h.player.sendSystemMessage(Component.literal("Nasıl oynanır? ").withStyle(ChatFormatting.GOLD).append(link("baslangic", TOPICS.get("baslangic"))).append(Component.literal("  ya da /twd help").withStyle(ChatFormatting.GRAY)));
        });
    }

    private TwdHelp() {}
}
