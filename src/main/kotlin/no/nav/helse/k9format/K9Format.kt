package no.nav.helse.k9format

import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.Tilsynsordning
import no.nav.k9.søknad.felles.*
import no.nav.k9.søknad.felles.Barn
import no.nav.k9.søknad.pleiepengerbarn.*
import no.nav.k9.søknad.pleiepengerbarn.Beredskap
import no.nav.k9.søknad.pleiepengerbarn.Utenlandsopphold
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.LocalDate

fun PreprossesertMeldingV1.tilK9PleiepengeBarnSøknad(): PleiepengerBarnSøknad {
    val språk = when (sprak) {
        "nb" -> Språk.NORSK_BOKMÅL
        "nn" -> Språk.NORSK_NYNORSK
        else -> Språk.NORSK_BOKMÅL
    }
    val builder = PleiepengerBarnSøknad.builder()
        .søknadId(SøknadId.of(soknadId))
        .mottattDato(mottatt)
        .språk(språk)
        .søker(soker.tilK9Søker())
        .arbeid(
            arbeidsgivere.tilK9Arbeid(
                frilans,
                Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(),
                NorskIdentitetsnummer.of(soker.fodselsnummer)
            )
        )
        .barn(barn.tilK9Barn())
        .søknadsperiode(Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(), SøknadsperiodeInfo())

    builder.bosteder(medlemskap.tilK9bosteder())

    beredskap?.let {
        builder.beredskap(
            Beredskap.builder().periode(
                Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(),
                Beredskap.BeredskapPeriodeInfo.builder().tilleggsinformasjon(beredskap.tilleggsinformasjon).build()
            ).build()
        )
    }

    utenlandsoppholdIPerioden?.let { oppholdIPerioden: UtenlandsoppholdIPerioden ->
        builder.utenlandsopphold(utenlandsoppholdIPerioden.tilK9Utenlandsopphold())
    }

    nattevaak?.let {
        builder.nattevåk(
            Nattevåk.builder()
                .periode(
                    Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build(),
                    Nattevåk.NattevåkPeriodeInfo.builder().tilleggsinformasjon(it.tilleggsinformasjon ?: "").build()
                )
                .build()
        )
    }

    tilsynsordning?.let {
        builder.tilsynsordning(tilsynsordning.tilK9Tilsynsordning(fraOgMed, tilOgMed))
    }

    ferieuttakIPerioden?.let {
        if (ferieuttakIPerioden.skalTaUtFerieIPerioden) {
            builder.lovbestemtFerie(ferieuttakIPerioden.tilK9LovbestemtFerie())
        }
    }

    return builder.build()
}

private fun FerieuttakIPerioden.tilK9LovbestemtFerie(): LovbestemtFerie {

    val perioder = mutableMapOf<Periode, LovbestemtFerie.LovbestemtFeriePeriodeInfo>()
    ferieuttak.forEach {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            LovbestemtFerie.LovbestemtFeriePeriodeInfo()
        )
    }

    return LovbestemtFerie.builder()
        .perioder(perioder)
        .build()
}


fun Medlemskap.tilK9bosteder(): Bosteder {
    val perioder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()
    utenlandsoppholdNeste12Mnd.forEach {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            Bosteder.BostedPeriodeInfo.builder().land(Landkode.of(it.landkode)).build()
        )
    }
    utenlandsoppholdSiste12Mnd.forEach {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            Bosteder.BostedPeriodeInfo.builder().land(Landkode.of(it.landkode)).build()
        )
    }
    return Bosteder.builder()
        .perioder(perioder)
        .build()
}

fun Tilsynsordning.tilK9Tilsynsordning(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate
): no.nav.k9.søknad.pleiepengerbarn.Tilsynsordning {
    val builder = no.nav.k9.søknad.pleiepengerbarn.Tilsynsordning.builder()
    return when {
        this.ja != null -> {
            val tilsynsordningSvar = when(svar) {
                "ja" -> TilsynsordningSvar.JA
                "nei" -> TilsynsordningSvar.NEI
                else -> throw IllegalArgumentException("Ikke gyldig tilsynsordningsvar. Forventet ja/nei, men fikk $svar")
            }
            builder
                .iTilsynsordning(tilsynsordningSvar)
                .uke(this.ja.tilK9TilsynsordningUke(fraOgMed, tilOgMed))
                .build()
        }
        this.vetIkke != null -> {
            builder.iTilsynsordning(TilsynsordningSvar.valueOf(svar)).build()
        }
        else -> builder.iTilsynsordning(TilsynsordningSvar.valueOf(svar)).build()
    }
}

fun TilsynsordningJa.tilK9TilsynsordningUke(fraOgMed: LocalDate, tilOgMed: LocalDate): TilsynsordningUke {
    val builder = TilsynsordningUke.builder()
    mandag?.let { builder.mandag(mandag) }
    tirsdag?.let { builder.tirsdag(tirsdag) }
    onsdag?.let { builder.onsdag(onsdag) }
    torsdag?.let { builder.torsdag(torsdag) }
    fredag?.let { builder.fredag(fredag) }

    return builder
        .periode(Periode.builder().fraOgMed(fraOgMed).tilOgMed(tilOgMed).build())
        .build()
}

fun UtenlandsoppholdIPerioden.tilK9Utenlandsopphold(): Utenlandsopphold? {
    val perioder = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
    opphold.forEach {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            Utenlandsopphold.UtenlandsoppholdPeriodeInfo.builder()
                .land(Landkode.of(it.landkode))
                .build()
        )
    }
    return Utenlandsopphold.builder()
        .perioder(perioder)
        .build()
}

fun PreprossesertBarn.tilK9Barn(): Barn {
    return when {
        !fodselsnummer.isNullOrBlank() -> Barn.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(fodselsnummer)).build()
        else -> Barn.builder().fødselsdato(fodselsdato).build()
    }
}

fun PreprossesertSoker.tilK9Søker(): Søker = Søker.builder()
    .norskIdentitetsnummer(NorskIdentitetsnummer.of(fodselsnummer))
    .build()

fun Arbeidsgivere.tilK9Arbeid(
    frilans: Frilans?,
    søknadsPeriode: Periode,
    norskIdentitetsnummer: NorskIdentitetsnummer
): Arbeid {

    val builder = Arbeid.builder()
        .arbeidstaker(organisasjoner.map { org ->
            Arbeidstaker.builder()
                .organisasjonsnummer(Organisasjonsnummer.of(org.organisasjonsnummer))
                .periode(
                    søknadsPeriode,
                    Arbeidstaker.ArbeidstakerPeriodeInfo.builder()
                        .skalJobbeProsent(BigDecimal.valueOf(org.skalJobbeProsent ?: 0.0))
                        .build()
                )
                .build()
        }.toMutableList())

    frilans?.let {
        builder.frilanser(frilans.tilK9Frilanser())
    }


    return builder.build()
}

private fun Frilans.tilK9Frilanser(): Frilanser {
    val perioder = mutableMapOf<Periode, Frilanser.FrilanserPeriodeInfo>()
    oppdrag.forEach {
        perioder.put(
            Periode.builder().fraOgMed(it.fraOgMed).tilOgMed(it.tilOgMed).build(),
            Frilanser.FrilanserPeriodeInfo()
        )
    }
    return Frilanser.builder()
        .perioder(perioder)
        .build()
}