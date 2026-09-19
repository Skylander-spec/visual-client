import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { installMockApi } from './mock'
import { bootAccent } from './components/SettingsModal'
import './theme.css'
import './epic.css'

installMockApi()
bootAccent()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
