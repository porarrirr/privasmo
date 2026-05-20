import Foundation

public class SensorDatabaseRepository {
    public init() {}
    
    public func loadSensors() -> Result<[SensorSpec], Error> {
        guard let url = Bundle.main.url(forResource: "sensor_database", withExtension: "csv") else {
            return .failure(NSError(
                domain: "SensorDatabaseRepository",
                code: 404,
                userInfo: [NSLocalizedDescriptionKey: "sensor_database.csv not found in bundle"]
            ))
        }
        do {
            let data = try Data(contentsOf: url)
            guard let raw = String(data: data, encoding: .utf8) else {
                return .failure(NSError(
                    domain: "SensorDatabaseRepository",
                    code: 500,
                    userInfo: [NSLocalizedDescriptionKey: "Failed to decode sensor_database.csv as UTF-8"]
                ))
            }
            let specs = parseSensorCsv(raw: raw)
            return .success(specs)
        } catch {
            return .failure(error)
        }
    }
}
