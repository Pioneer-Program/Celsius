package cute.ame.celsius.Fluid.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpeciesDefinition
(
    double molarMassGPerMol,
    float specificHeat,
    float boilingPointK,
    float latentHeatJPerKg
)
{
    public static final SpeciesDefinition FALLBACK = new SpeciesDefinition(28.97, 1005.0f, 78.9f, 199_000.0f);

    public static final Codec<SpeciesDefinition> CODEC = RecordCodecBuilder.create(i ->
        i.group(
            Codec.DOUBLE.fieldOf("molar_mass").forGetter(SpeciesDefinition::molarMassGPerMol),
            Codec.FLOAT.optionalFieldOf("specific_heat", 1005.0f).forGetter(SpeciesDefinition::specificHeat),
            Codec.FLOAT.optionalFieldOf("boiling_point", 78.9f).forGetter(SpeciesDefinition::boilingPointK),
            Codec.FLOAT.optionalFieldOf("latent_heat", 199_000.0f).forGetter(SpeciesDefinition::latentHeatJPerKg)
        ).apply(i, SpeciesDefinition::new)
    );
}
