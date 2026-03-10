# ConfigLens-Nexus

Smart YAML, JSON, and .env Editor with Path-Copying, Hierarchical Data Map & Env-Injection

**ConfigLens** is an intelligent, high-performance Eclipse IDE plugin that supercharges your experience working with complex configuration files. Specifically designed for massive YAML, JSON, and Properties structures, it offers real-time secret detection, AI-powered contextual tooltips, and deeply integrated visual features that help you understand your config faster.

 
> *Explore giant configs without losing your context.*
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/525d4b84-840c-448f-8f8b-2c996544b928" />
---

## 🌟 Key Features

1. **Path-Copying (Dot-Notation Generator)**
   Instantly copy the full dot-notation path of any configuration node directly to your clipboard. No more error-prone manual path typing.

2. **Env-Injection & Ghost Text**
   Automatically overlays resolved environment variables right alongside your configuration values via intuitive "ghost text." Understand which local environment strings are overriding your config instantly.

3. **Flat View Console**
   Easily toggle a flattened, property-like view of deeply nested YAML or JSON keys, making global search and comparisons radically simpler.

4. **Secret Highlighting & Redaction**
   On-the-fly detection of high-entropy strings and potentially sensitive tokens, using sophisticated heuristics to help keep your secrets out of source control.

5. **AI Analysis Context (Gemini Pro integration)**
   Select config segments and instantly request AI explanations of the node's purpose based on its context within the file using our Gemini API integration.

## 🚀 Installation Instructions

ConfigLens is distributed as an Eclipse p2 update site, making installation quick and seamless within your Eclipse instance.

**Update Site URL:** `https://VinayK8866.github.io/ConfigLens-Nexus/`

### Steps to Install in Eclipse:
1. Open Eclipse.
2. Go to **Help** -> **Install New Software...**
3. Click the **Add...** button to add a new repository.
4. Name: `ConfigLens`
   Location: `https://VinayK8866.github.io/ConfigLens-Nexus/`
5. Select "ConfigLens Feature" in the resulting list.
6. Click **Next** and follow the prompts. Restart Eclipse when asked.

## 🛠 Prerequisites
- Java 17+
- Eclipse IDE (2023-09 or newer recommended)

## 📄 License
This project is licensed under the [Eclipse Public License v2.0 (EPL)](http://www.eclipse.org/legal/epl-v20.html). 
See the `LICENSE` file for details.
