struct DRMParams {
    let type: String?
    let licenseServer: String?
    let headers: [AnyHashable : Any]?
    let contentId: String?
    let certificateUrl: String?
    let base64Certificate: Bool?
    
    let json: NSDictionary?
    
    init(_ json: NSDictionary!) {
        self.json = json
        self.type = json["type"] as? String
        self.licenseServer = json["licenseServer"] as? String
        self.contentId = json["contentId"] as? String
        self.certificateUrl = json["certificateUrl"] as? String
        self.base64Certificate = json["base64Certificate"] as? Bool
        self.headers = json["headers"] as? [AnyHashable : Any]
    }
}
