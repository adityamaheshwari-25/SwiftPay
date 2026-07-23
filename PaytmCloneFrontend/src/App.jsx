import { Suspense, lazy } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom"
import { AuthProvider } from "./context/AuthContext"
import "./index.css";

import ProtectedRoute from "./components/auth/ProtectedRoute";
import { Toaster } from "sonner";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { PublicRoute } from "./components/auth/PublicRoute";
import { APP_ROLES } from "./config/roles.config";

const LandingPage = lazy(() => import("./pages/LandingPage"));
const LoginPage = lazy(() => import("./pages/LoginPage").then((m) => ({ default: m.LoginPage })));
const RegisterUserPage = lazy(() => import("./pages/RegisterUserPage").then((m) => ({ default: m.RegisterUserPage })));
const RegisterMerchantPage = lazy(() => import("./pages/RegisterMerchantPage").then((m) => ({ default: m.RegisterMerchantPage })));
const UserDashboard = lazy(() => import("./pages/UserDashboard"));
const MerchantDashboard = lazy(() => import("./pages/MerchantDashboard").then((m) => ({ default: m.MerchantDashboard })));
const Unauthorized = lazy(() => import("./pages/Unauthorized"));
const AdminKycPage = lazy(() => import("./pages/AdminKycPage").then((m) => ({ default: m.AdminKycPage })));
const TransactionsPage = lazy(() => import("./pages/TransactionsPage"));
const MerchantTransactionsPage = lazy(() => import("./pages/MerchantTransactionsPage").then((m) => ({ default: m.MerchantTransactionsPage })));
const SettlementHistoryPage = lazy(() => import("./pages/SettlementHistoryPage").then((m) => ({ default: m.SettlementHistoryPage })));
const AdminLayout = lazy(() => import("./components/layout/AdminLayout").then((m) => ({ default: m.AdminLayout })));
const NotFound = lazy(() => import("./pages/NotFound"));
const HighValueMerchantTransactionsPage = lazy(() => import("./pages/admin/HighValueMerchantTransactionsPage"));
const HighValueMerchantsSummaryPage = lazy(() => import("./pages/admin/HighValueMerchantSummaryPage"));


/**
 * creating the QueryClient, this stale time is at the global level, but for the individual hook(that we have created in the hooks folder) 
 * the staleTime we have overrides this global one.
 * 
 * Rule: The query’s own options win over global defaults.
 * 
 * If you mount the same query in two places with different staleTimes, React Query uses the configuration of the 
 * active observers; results can be surprising.
 * 
 * Best practice: keep staleTime consistent per query key, ideally defined in one place (custom hook).
 * 
 * React Query resolves options like this:

        1. Start with library defaults

        2. Apply QueryClient defaultOptions

        3. Override with options passed to useQuery

  If a property is:

      Defined in useQuery → it overrides

      Not defined in useQuery → it inherits from QueryClient defaultOptions

      That’s what “merged” means.

  React Query merges per-query options with global defaults. Per-query options override global ones, while unspecified 
  options inherit from the QueryClient configuration.
 * 
 * */ 

// these are the QueryClient defaultOptions.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 30,
      retry: 1,
      refetchOnWindowFocus: true,
    },
  },
});



function RouteFallback() {
  return (
    <div className="flex min-h-[40vh] items-center justify-center text-sm text-muted-foreground">
      Loading page...
    </div>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
    <BrowserRouter>
      <AuthProvider>
        <Suspense fallback={<RouteFallback />}>
          <Routes>
            {/* Public Routes */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
            <Route path="/register-user" element={<PublicRoute><RegisterUserPage /></PublicRoute>} />
            <Route path="/register-merchant" element={<PublicRoute><RegisterMerchantPage /></PublicRoute>} />
            <Route
              path="/user/dashboard"
              element={
                <ProtectedRoute allowedRoles={[APP_ROLES.USER]}>
                  <UserDashboard />
                </ProtectedRoute>
              }
            />

            {/* --- MERCHANT ONLY ROUTES --- */}
            <Route
              path="/merchant/dashboard"
              element={
                <ProtectedRoute allowedRoles={[APP_ROLES.MERCHANT]}>
                  <MerchantDashboard />
                </ProtectedRoute>
              }
            />

            <Route
              path="/merchant/transactions"
              element={
                <ProtectedRoute allowedRoles={[APP_ROLES.MERCHANT]}>
                  <MerchantTransactionsPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/merchant/settlements"
              element={
                <ProtectedRoute allowedRoles={[APP_ROLES.MERCHANT]}>
                  <SettlementHistoryPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/user/transactions"
              element={
                <ProtectedRoute allowedRoles={[APP_ROLES.USER]}>
                  <TransactionsPage />
                </ProtectedRoute>
              }
            />

            <Route path="/unauthorized" element={<Unauthorized />} />
            {/* <Route path="/send-money" element={<SendMoney />} /> */}
            {/* <Route path="/add-bankAccount" element={<TestPage />} /> */}
            {/* <Route path="/test-bank" element={<TestBankAccounts />} /> */}
            <Route path="/userdash-test" element={<UserDashboard />} />
            {/* <Route path="/admin" element={<AdminKycPage/>} /> */}
            {/* <Route path="/admin" element={<AdminKycPage/>} /> */}
          {/* --- ADMIN ROUTES (Sidebar + Header based) --- */}
          <Route element={<ProtectedRoute allowedRoles={[APP_ROLES.ADMIN]}><AdminLayout /></ProtectedRoute>}>
            {/* <Route path="/admin/dashboard" element={<AdminDashboard />} /> */}
            <Route path="/admin/kyc" element={<AdminKycPage />} />
            <Route path="/admin/high-value-merchants" element={<HighValueMerchantsSummaryPage />} />
            <Route path="/admin/high-value-merchants/:merchantId" element={<HighValueMerchantTransactionsPage />} />
          </Route>

          {/* <Route path="/admin/dashboard" element={<AdminDashboard />} /> */}


          <Route path="*" element={<NotFound/>}/>

          </Routes>
        </Suspense>
      </AuthProvider>
    </BrowserRouter>
    <Toaster position="top-center" richColors/>
    
    {/* devtools only visible in development mode*/}
    <ReactQueryDevtools initialIsOpen={false}/>
    </QueryClientProvider>
  )
}

export default App
