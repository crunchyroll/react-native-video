import AVFoundation

class RCTResourceLoaderDelegate: NSObject, AVAssetResourceLoaderDelegate, URLSessionDelegate {
    
    private var _loadingRequest:AVAssetResourceLoadingRequest?
    private var _requestingCertificate:Bool = false
    private var _requestingCertificateErrored:Bool = false
    private var _drm: NSDictionary?
    private var _reactTag: NSNumber?
    private var _onVideoError: RCTDirectEventBlock?
    private var _onGetLicense: RCTDirectEventBlock?
    
    
    init(
        asset: AVURLAsset,
        drm: NSDictionary?,
        onVideoError: RCTDirectEventBlock?,
        onGetLicense: RCTDirectEventBlock?,
        reactTag: NSNumber
    ) {
        super.init()
        let queue = DispatchQueue(label: "assetQueue")
        asset.resourceLoader.setDelegate(self, queue: queue)
        _reactTag = reactTag
        _onVideoError = onVideoError
        _onGetLicense = onGetLicense
        _drm = drm
    }
    
    deinit {
        _loadingRequest?.finishLoading()
    }
    
    func resourceLoader(_ resourceLoader:AVAssetResourceLoader!, shouldWaitForRenewalOfRequestedResource renewalRequest:AVAssetResourceRenewalRequest!) -> Bool {
        return loadingRequestHandling(renewalRequest)
    }
    
    func resourceLoader(_ resourceLoader:AVAssetResourceLoader!, shouldWaitForLoadingOfRequestedResource loadingRequest:AVAssetResourceLoadingRequest!) -> Bool {
        return loadingRequestHandling(loadingRequest)
    }
    
    func resourceLoader(_ resourceLoader:AVAssetResourceLoader!, didCancelLoadingRequest loadingRequest:AVAssetResourceLoadingRequest!) {
        NSLog("didCancelLoadingRequest")
    }
    
    func base64DataFromBase64String(base64String:String!) -> NSData! {
        if base64String != nil {
            // NSData from the Base64 encoded str
            let base64Data:NSData! = NSData.init(base64Encoded:base64String)
            return base64Data
        }
        return nil
    }
    
    func setLicenseResult(_ license:String!) {
        guard let respondData:NSData? = self.base64DataFromBase64String(base64String: license),
              let _loadingRequest = _loadingRequest else {
                  setLicenseResultError("No data from JS license response")
                  return
              }
        let dataRequest:AVAssetResourceLoadingDataRequest! = _loadingRequest.dataRequest
        dataRequest.respond(with: respondData as! Data)
        _loadingRequest.finishLoading()
    }
    
    func setLicenseResultError(_ error:String!) {
        if _loadingRequest != nil {
            self.finishLoadingWithError(error: RCTVideoErrorHandler.fromJSPart(error))
        }
    }
    
    func finishLoadingWithError(error:NSError!) -> Bool {
        if let _loadingRequest = _loadingRequest, let error = error {
            let licenseError:NSError! = error
            _loadingRequest.finishLoading(with: licenseError)
            
            _onVideoError?([
                "error": [
                    "code": NSNumber(value: error.code),
                    "localizedDescription": error.localizedDescription == nil ? "" : error.localizedDescription,
                    "localizedFailureReason": ((error as NSError).localizedFailureReason == nil ? "" : (error as NSError).localizedFailureReason) ?? "",
                    "localizedRecoverySuggestion": ((error as NSError).localizedRecoverySuggestion == nil ? "" : (error as NSError).localizedRecoverySuggestion) ?? "",
                    "domain": (error as NSError).domain
                ],
                "target": _reactTag
            ])
            
        }
        return false
    }
    
    func loadingRequestHandling(_ loadingRequest:AVAssetResourceLoadingRequest!) -> Bool {
        if _requestingCertificate {
            return true
        } else if _requestingCertificateErrored {
            return false
        }
        _loadingRequest = loadingRequest
        
        let url = loadingRequest.request.url
        guard let _drm = _drm else {
            return finishLoadingWithError(error: RCTVideoErrorHandler.noDRMData)
        }
        
        var contentId:String!
        let contentIdOverride:String! = _drm["contentId"] as? String
        if contentIdOverride != nil {
            contentId = contentIdOverride
        } else if (_onGetLicense != nil) {
            contentId = url?.host
        } else {
            contentId = url?.absoluteString.replacingOccurrences(of: "skd://", with:"")
        }
        
        let drmType:String! = _drm["type"] as? String
        guard drmType == "fairplay" else {
            return finishLoadingWithError(error: RCTVideoErrorHandler.noDRMData)
        }
        
        let certificateStringUrl:String! = _drm["certificateUrl"] as? String
        guard let certificateStringUrl = certificateStringUrl, let certificateURL = URL(string: certificateStringUrl.addingPercentEncoding(withAllowedCharacters: .urlFragmentAllowed) ?? "") else {
            return finishLoadingWithError(error: RCTVideoErrorHandler.noCertificateURL)
        }
        DispatchQueue.global(qos: .default)
        do {
            var certificateData:Data? = try Data(contentsOf: certificateURL)
            // 1255 bytes - same
            if (_drm["base64Certificate"] != nil) {
                certificateData = Data(base64Encoded: certificateData! as Data, options: .ignoreUnknownCharacters)
            }
            
            guard let certificateData = certificateData else {
                finishLoadingWithError(error: RCTVideoErrorHandler.noCertificateData)
                _requestingCertificateErrored = true
                return true
            }
            
            var contentIdData:NSData!
            if _onGetLicense != nil {
                contentIdData = contentId.data(using: .utf8) as NSData?
            } else {
                contentIdData = NSData(bytes: contentId.cString(using: String.Encoding.utf8), length:contentId.lengthOfBytes(using: String.Encoding.utf8))
                // 48 bytes
            }
            
            let dataRequest:AVAssetResourceLoadingDataRequest! = loadingRequest.dataRequest
            guard dataRequest != nil else {
                finishLoadingWithError(error: RCTVideoErrorHandler.noCertificateData)
                _requestingCertificateErrored = true
                return true
            }
            
            var spcError:NSError! = nil
            var spcData: Data? = nil
            do {
                spcData = try loadingRequest.streamingContentKeyRequestData(forApp: certificateData as Data, contentIdentifier: contentIdData as Data, options: nil)
                // 7148 bytes
            } catch let spcError {
                print("SPC error")
            }
            // Request CKC to the server
            var licenseServer:String! = _drm["licenseServer"] as? String
            if spcError != nil {
                finishLoadingWithError(error: spcError)
                _requestingCertificateErrored = true
            }
            
            guard spcData != nil else {
                finishLoadingWithError(error: RCTVideoErrorHandler.noSPC)
                _requestingCertificateErrored = true
                return true
            }
            
            // js client has a onGetLicense callback and will handle license fetching
            if let _onGetLicense = _onGetLicense {
                let base64Encoded = spcData?.base64EncodedString(options: [])
                _requestingCertificate = true
                if licenseServer == nil {
                    licenseServer = ""
                }
                _onGetLicense(["licenseUrl": licenseServer,
                              "contentId": contentId,
                              "spcBase64": base64Encoded,
                              "target": _reactTag])
                
                // license fetching will be handled inside RNV with the given parameters
            } else if licenseServer != nil {
                let request:NSMutableURLRequest! = NSMutableURLRequest()
                request.httpMethod = "POST"
                request.url = NSURL(string: licenseServer) as URL?
                // HEADERS
                let headers = _drm["headers"] as? [AnyHashable : Any]
                if let headers = headers {
                    for key in headers {
                        guard let key = key as? String else {
                            continue
                        }
                        let value = headers[key] as? String
                        request.setValue(value, forHTTPHeaderField: key)
                    }
                }
                
                if (_onGetLicense != nil) {
                    request.httpBody = spcData
                } else {
                    let spcEncoded = spcData?.base64EncodedString(options: [])
                    let spcUrlEncoded = CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault, spcEncoded as? CFString? as! CFString, nil, "?=&+" as CFString, CFStringBuiltInEncodings.UTF8.rawValue) as? String
                    let post:String! = String(format:"spc=%@&%@", spcUrlEncoded as! CVarArg, contentId)
                    let postData:NSData! = post.data(using: String.Encoding.utf8, allowLossyConversion:true) as NSData?
                    request.httpBody = postData as Data?
                }
                
                let configuration:URLSessionConfiguration! = URLSessionConfiguration.default
                let session:URLSession! = URLSession(configuration: configuration, delegate:self, delegateQueue:nil)
                let postDataTask:URLSessionDataTask! = session.dataTask(with: request as URLRequest, completionHandler:{ [weak self] (data:Data!,response:URLResponse!,error:Error!) in
                    guard let self = self else { return }
                    let httpResponse:HTTPURLResponse! = response as! HTTPURLResponse
                    if error != nil {
                        print("Error getting license from \(url?.absoluteString), HTTP status code \(httpResponse.statusCode)")
                        self.finishLoadingWithError(error: error as NSError?)
                        self._requestingCertificateErrored = true
                    } else {
                        if httpResponse.statusCode != 200 {
                            print("Error getting license from \(url?.absoluteString), HTTP status code \(httpResponse.statusCode)")
                            self.finishLoadingWithError(error: RCTVideoErrorHandler.licenseRequestNotOk(httpResponse.statusCode))
                            self._requestingCertificateErrored = true
                        } else if data != nil {
                            if (self._onGetLicense != nil) {
                                dataRequest.respond(with: data)
                            } else {
                                let decodedData = Data(base64Encoded: data, options: [])
                                if let decodedData = decodedData {
                                    dataRequest.respond(with: decodedData)
                                }
                            }
                            loadingRequest.finishLoading()
                        } else {
                            self.finishLoadingWithError(error: RCTVideoErrorHandler.noDataFromLicenseRequest)
                            self._requestingCertificateErrored = true
                        }
                        
                    }
                })
                postDataTask.resume()
            }
            
        } catch {
        }
        return true
    }
}
