import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import TemplateComponent from './projectcreate/TemplateComponent.jsx';
import NameComponent from './projectcreate/NameComponent.jsx';
import DestinationStorageComponent from './projectcreate/DestinationStorageComponent.jsx';
import ProjectCompletionComponent from "./projectcreate/CompletionComponent.jsx";
import PlutoLinkageComponent from './projectcreate/PlutoLinkageComponent.jsx';
import MediaRulesComponent from './projectcreate/MediaRulesComponent.jsx';

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
            lastError: null,
            deletable: false,
            deep_archive: true,
            sensitive: false
        };

        this.templateSelectionUpdated = this.templateSelectionUpdated.bind(this);
        this.nameSelectionUpdated = this.nameSelectionUpdated.bind(this);
        this.storageSelectionUpdated = this.storageSelectionUpdated.bind(this);
        this.plutoDataUpdated = this.plutoDataUpdated.bind(this);
        this.rulesDataUpdated = this.rulesDataUpdated.bind(this);
    }

    requestDefaultProjectStorage(defaultValue){
        return new Promise((resolve,reject)=>axios.get("/api/default/project_storage_id")
            .then(response=>{
                const defaultStorage = parseInt(response.data.result.value);
                console.log("Got default storage of ", defaultStorage);
                resolve(defaultStorage);
            }).catch(error=>{
                if(error.response && error.response.status===404){
                    console.log("No default storage has been set");
                    resolve(defaultValue);
                } else {
                    console.error(error);
                    this.setState({lastError: error})
                }
            })
        );
    }

    componentWillMount(){
        Promise.all([
            axios.get("/api/template"),
            axios.get("/api/storage"),
            axios.get("/api/pluto/workinggroup")
        ]).then(responses=>{
            const firstTemplate = responses[0].data.result[0] ? responses[0].data.result[0].id : null;
            const firstStorage = responses[1].data.result[0] ? responses[1].data.result[0].id : null;
            const firstWorkingGroup = responses[2].data.result.length ? responses[2].data.result[0].id : null;

            this.requestDefaultProjectStorage(firstStorage).then(projectStorage=>
                this.setState({
                    projectTemplates: responses[0].data.result, selectedProjectTemplate: firstTemplate,
                    storages: responses[1].data.result, selectedStorage: projectStorage,
                    wgList: responses[2].data.result, selectedWorkingGroup: firstWorkingGroup
                })
            );
        }).catch(error=>{
            console.error(error);
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

    rulesDataUpdated(newdata){
        this.setState({
            deletable: newdata.deletable,
            deep_archive: newdata.deep_archive,
            sensitive: newdata.sensitive
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
                name: "Media Rules",
                component: <MediaRulesComponent valueWasSet={this.rulesDataUpdated}
                                                deletable={this.state.deletable}
                                                deep_archive={this.state.deep_archive}
                                                sensitive={this.state.sensitive}
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
