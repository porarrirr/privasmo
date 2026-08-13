# Smartphone Sensor Comparison

English | [日本語](README.ja.md)

An Android app for comparing smartphone camera sensors, apertures, and 35 mm-equivalent focal lengths. It estimates the effective sensor area used at a given field of view and a theoretical relative-light index, making camera specifications easier to compare side by side.

Public page: https://porarrirr.github.io/sumaho-hikaku/

## Highlights

- Compare up to five phones with four lenses each
- Separate optical-zoom ranges from digital-crop ranges
- Enter custom sensors using familiar fractions such as `1/1.28`
- Save and restore comparison presets
- Review a numeric summary and export results

## Understanding the result

The app's total-light value is a dimensionless comparison index, not a measured lumen value or a guarantee of real-world image quality. It is based on effective sensor area and f-number:

$$L_{relative}=\frac{A_{effective}}{N^2}$$

The estimate does not model lens transmission, quantum efficiency, color filters, read noise, HDR processing, exposure time, stabilization, vignetting, or manufacturer image processing. Use it to compare geometry and aperture under shared assumptions, then confirm important decisions with sample images and primary specifications.

Sensor data is stored in [`app/src/main/res/raw/sensor_database.csv`](app/src/main/res/raw/sensor_database.csv). Because the dataset does not currently retain a primary-source URL for every row, verify values against manufacturer documentation or other primary sources before citing them.

## Build

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

## License

The original code is proprietary and all rights are reserved. See [LICENSE](LICENSE). Third-party components remain subject to their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
