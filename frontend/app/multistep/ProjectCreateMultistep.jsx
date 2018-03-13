import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import TemplateComponent from './projectcreate/TemplateComponent.jsx';
import NameComponent from './projectcreate/NameComponent.jsx';
import DestinationStorageComponent from './projectcreate/DestinationStorageComponent.jsx';
import ProjectCompletionComponent from "./projectcreate/CompletionComponent.jsx";
import PlutoLinkageComponent from './projectcreate/PlutoLinkageComponent.jsx';

class ProjectCreateMultistep extends React.Component {
    static propTypes = {

    };

    constructor(props){
        super(props);

        this.state = {
            projectTemplates: [],
            selectedProjectTemplate: null,
            storages: [],
            wgList: [],
            selectedWorkingGroup:null,
            selectedCommissionId:null,
            selectedStorage: null,
            projectName: "",
            projectFilename: "",
            lastError: null
        };

        this.templateSelectionUpdated = this.templateSelectionUpdated.bind(this);
        this.nameSelectionUpdated = this.nameSelectionUpdated.bind(this);
        this.storageSelectionUpdated = this.storageSelectionUpdated.bind(this);
        this.plutoDataUpdated = this.plutoDataUpdated.bind(this);
    }

    componentWillMount(){
        axios.get("/api/template")
            .then(response=>{
                const firstTemplate = response.data.result[0] ? response.data.result[0].id : null;
                this.setState({projectTemplates: response.data.result, selectedProjectTemplate: firstTemplate});
            })
            .catch(error=>{
                console.error(error);
                this.setState({lastError: error});
            });

        axios.get("/api/storage")
            .then(response=>{
                const firstStorage = response.data.result[0] ? response.data.result[0].id : null;
                this.setState({storages: response.data.result, selectedStorage: firstStorage});
            })
            .catch(error=>{
                console.error(error);
                this.setState({lastError: error});
            });

        axios.get("/api/pluto/workinggroup").then(response=>{
            this.setState({wgList: response.data.result, selectedWorkingGroup: response.data.result.length ? response.data.result[0].id : null});
        }).catch(error=>{
            this.setState({lastError: error});
        });
    }

    templateSelectionUpdated(newTemplate, cb){
        this.setState({selectedProjectTemplate: newTemplate}, cb);
    }

    nameSelectionUpdated(newNameState){
        this.setState({projectName: newNameState.projectName, projectFilename: newNameState.fileName});
    }

    storageSelectionUpdated(newStorage){
        this.setState({selectedStorage: newStorage});
    }

    plutoDataUpdated(newdata){
        this.setState({
            selectedWorkingGroup: newdata.workingGroupRef,
            selectedCommissionId: newdata.plutoCommissionRef
        })
    }

    render(){
        const steps = [
            {
                name: "Select project template",
                component: <TemplateComponent templatesList={this.state.projectTemplates}
                                              selectedTemplate={this.state.selectedProjectTemplate}
                                              selectionUpdated={this.templateSelectionUpdated}/>
            },
            {
                name: "Name your project",
                component: <NameComponent projectName={this.state.projectName}
                                          fileName={this.state.fileName}
                                          selectionUpdated={this.nameSelectionUpdated}/>
            },
            {
                name: "Working Group & Commission",
                component: <PlutoLinkageComponent valueWasSet={this.plutoDataUpdated}
                                                  workingGroupList={this.state.wgList}
                                                  currentWorkingGroup={this.state.selectedWorkingGroup }
                />
            },
            {
                name: "Destination storage",
                component: <DestinationStorageComponent storageList={this.state.storages}
                                                        selectedStorage={this.state.selectedStorage}
                                                        selectionUpdated={this.storageSelectionUpdated}/>
            },
            {
                name: "Summary",
                component: <ProjectCompletionComponent projectTemplates={this.state.projectTemplates}
                                             selectedProjectTemplate={this.state.selectedProjectTemplate}
                                             storages={this.state.storages}
                                             selectedStorage={this.state.selectedStorage}
                                             projectName={this.state.projectName}
                                             projectFilename={this.state.projectFilename}
                                             selectedWorkingGroupId={this.state.selectedWorkingGroup}
                                             selectedCommissionId={this.state.selectedCommissionId}
                                             wgList={this.state.wgList}
                />
            }
        ];

        return <Multistep showNavigation={true} steps={steps}/>
    }
}

export default ProjectCreateMultistep;
