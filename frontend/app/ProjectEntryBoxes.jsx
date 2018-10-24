import React from 'react';
import GeneralBoxComponent from './GeneralBoxComponent.jsx'
import ProjectTypeView from './EntryViews/ProjectTypeView.jsx';
import ProjectEntryFilterComponent from './filter/ProjectEntryFilterComponent.jsx';
import WorkingGroupEntryView from './EntryViews/WorkingGroupEntryView.jsx';
import CommissionEntryView from './EntryViews/CommissionEntryView.jsx';
import PropTypes from 'prop-types';
import axios from 'axios';

import ProjectBoxLeftComponent from './ProjectBoxLeftComponent.jsx'
import ProjectBoxMiddleComponent from './ProjectBoxMiddleComponent.jsx'
import ProjectBoxRightComponent from './ProjectBoxRightComponent.jsx'


class ProjectEntryBoxes extends GeneralBoxComponent {
    static propTypes = {
        location: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);
        this.endpoint = '/api/project';
        this.filterEndpoint = '/api/project/list';
        this.getPlutoLink = this.getPlutoLink.bind(this);
        this.componentWillMount = this.componentWillMount.bind(this);

        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralBoxComponent.standardColumn("Title","title"),
            {
                header: "Pluto project",
                key: "vidispineId",
                render: this.getPlutoLink,
                headerProps: { className: 'dashboardheader'}
            },
            {
                header: "Project type",
                key: "projectTypeId",
                render: (typeId)=><ProjectTypeView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            GeneralBoxComponent.dateTimeColumn("Created", "created"),
            GeneralBoxComponent.standardColumn("Owner","user"),
            {
                header: "Working group",
                key: "workingGroupId",
                render: typeId=><WorkingGroupEntryView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            {
                header: "Commission",
                key: "commissionId",
                render: typeId=><CommissionEntryView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            this.actionIcons(),
            {
                header: "",
                key: "id",
                headerProps: {className: 'dashboardheader'},
                render: projid=><a target="_blank" href={"pluto:openproject:" + projid}>Open project</a>
            }
        ];
    }

    componentWillMount(){
        axios.get("/api/system/plutoconfig")
            .then(response=>this.setState({plutoConfig: response.data}))
            .catch(error=>console.error(error));
    }
    //
    getPlutoLink(vsid){
        if(!vsid) return <span className="value-not-present">(not set)</span>;

        if(this.state.plutoConfig.hasOwnProperty("plutoServer")){
            const baseUrl = this.state.plutoConfig.plutoServer + "/project/";
            return <a target="_blank" href={baseUrl + vsid}>{vsid}</a>
        } else {
            return <b>{vsid}</b>
        }
    }

    dependenciesDidLoad(){
        this.setState({filterTerms: this.props.location.search.includes("mine") ? {user: this.state.uid, match: "W_EXACT"} : {match: "W_CONTAINS"}})
    }

    getFilterComponent(){
        return <ProjectEntryFilterComponent filterTerms={this.state.filterTerms} filterDidUpdate={this.filterDidUpdate}/>
    }

    newElementCallback(event) {
        this.props.history.push("/project/new");
    }
}

export default ProjectEntryBoxes;