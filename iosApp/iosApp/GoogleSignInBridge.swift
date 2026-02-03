//
//  GoogleSignInBridge.swift
//  iosApp
//
//  Created by Bhaskar Dey on 01/02/26.
//

import Foundation
import GoogleSignIn
import UIKit
import ComposeApp

public class GoogleSignInBridge {

    public static let shared = GoogleSignInBridge()
    private var timer: Timer?

    private init() {
        startPolling()
    }

    private func startPolling() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
            if GoogleSignInState.shared.shouldTrigger {
                if let clientId = GoogleSignInState.shared.clientId {
                    GoogleSignInState.shared.shouldTrigger = false
                    self.performSignIn(clientId: clientId)
                }
            }
        }
    }

    private func performSignIn(clientId: String) {
        DispatchQueue.main.async {
            var rootVC: UIViewController? = nil

            for scene in UIApplication.shared.connectedScenes {
                if let ws = scene as? UIWindowScene {
                    rootVC = ws.windows.first?.rootViewController
                    if rootVC != nil {
                        break
                    }
                }
            }

            if rootVC == nil {
                GoogleSignInState.shared.onFailure(error: "No root view controller")
                return
            }

            let config = GIDConfiguration(clientID: clientId)
            GIDSignIn.sharedInstance.configuration = config

            GIDSignIn.sharedInstance.signIn(withPresenting: rootVC!) { result, error in
                if error != nil {
                    GoogleSignInState.shared.onFailure(error: error!.localizedDescription)
                    return
                }

                if result == nil {
                    GoogleSignInState.shared.onFailure(error: "No result")
                    return
                }

                let user = result!.user

                // Get idToken (optional)
                guard let idToken = user.idToken?.tokenString else {
                    GoogleSignInState.shared.onFailure(error: "No idToken")
                    return
                }

                // Get accessToken (NOT optional - direct access)
                let accessToken = user.accessToken.tokenString

                let name = user.profile?.name
                var picUrl: String? = nil
                if let img = user.profile?.imageURL(withDimension: 200) {
                    picUrl = img.absoluteString
                }

                GoogleSignInState.shared.onSuccess(
                    idToken: idToken,
                    accessToken: accessToken,
                    displayName: name,
                    profilePicUrl: picUrl
                )
            }
        }
    }
}
