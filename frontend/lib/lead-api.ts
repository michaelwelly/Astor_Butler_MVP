export type LeadRequest = {
  projectType: string;
  description: string;
  services: string;
  format: string;
  deadline: string;
  budget: string;
  contact: string;
};

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8088";

export async function submitLead(payload: LeadRequest) {
  const endpoint = process.env.NEXT_PUBLIC_LEAD_ENDPOINT;

  if (!endpoint) {
    await new Promise((resolve) => setTimeout(resolve, 650));
    return { status: "demo", payload };
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Lead request failed");
  }

  return response.json();
}
