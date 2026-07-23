import { ShieldAlert } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { LABELS } from "@/config/labels.config";

const Unauthorized = () => {
  const labels = LABELS.pages.unauthorizedPage;
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="bg-white p-8 rounded-2xl shadow-lg text-center max-w-md w-full">
        <div className="mx-auto mb-4 w-12 h-12 flex items-center justify-center rounded-full bg-red-100">
          <ShieldAlert className="text-red-600 w-6 h-6" />
        </div>

        <h1 className="text-2xl font-bold text-slate-900 mb-2">{labels.title}</h1>

        <p className="text-slate-600 mb-6">{labels.description}</p>

        <button
          onClick={() => navigate("/")}
          className="w-full py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium"
        >
          {labels.goToHome}
        </button>
      </div>
    </div>
  );
};

export default Unauthorized;
