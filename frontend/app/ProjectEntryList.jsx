import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';
import ProjectEntryFiles from './ProjectEntryFiles.jsx';
import ProjectTypeView from './EntryViews/ProjectTypeView.jsx';
import ProjectEntryFilterComponent from './filter/ProjectEntryFilterComponent.jsx';
import WorkingGroupEntryView from './EntryViews/WorkingGroupEntryView.jsx';
import CommissionEntryView from './EntryViews/CommissionEntryView.jsx';
import PropTypes from 'prop-types';
import axios from 'axios';

class ProjectEntryList extends GeneralListComponent {
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
            GeneralListComponent.standardColumn("Title","title"),
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
            GeneralListComponent.dateTimeColumn("Created", "created"),
            GeneralListComponent.standardColumn("Owner","user"),
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
        console.log("loading pluto config");
        axios.get("/api/system/plutoconfig")
            .then(response=>this.setState({plutoConfig: response.data}))
            .catch(error=>console.error(error));
    }
    //
    getPlutoLink(vsid){
        console.log(this);
        console.log(vsid);

        if(!vsid) return <span className="value-not-present">(not set)</span>;

        if(this.state.plutoConfig.hasOwnProperty("plutoServer")){
            const baseUrl = this.state.plutoConfig.plutoServer + "/project/";
            return <a target="_blank" href={baseUrl + vsid}>{vsid}</a>
        } else {
            return <b>{vsid}</b>
        }
    }

    getFilterComponent(){
        return <ProjectEntryFilterComponent filterDidUpdate={this.filterDidUpdate}/>
    }

    newElementCallback(event) {
        this.props.history.push("/project/new");
    }
}

export default ProjectEntryList;